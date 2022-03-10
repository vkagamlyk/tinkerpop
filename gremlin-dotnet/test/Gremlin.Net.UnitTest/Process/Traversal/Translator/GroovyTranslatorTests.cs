#region License

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#endregion

using System;
using System.Collections.Generic;
using Gremlin.Net.Process.Traversal;
using Gremlin.Net.Process.Traversal.Strategy.Decoration;
using Gremlin.Net.Process.Traversal.Strategy.Verification;
using Gremlin.Net.Process.Traversal.Translator;
using Gremlin.Net.Structure;
using Xunit;

namespace Gremlin.Net.UnitTest.Process.Traversal.Translator;

public class GroovyTranslatorTests
{
    private readonly GraphTraversalSource _g = AnonymousTraversalSource.Traversal();
    
    [Fact]
    public void ShouldTranslateStepsWithSingleArguments()
    {
        var translator = CreateTranslator();

        var translated = translator.Translate(_g.V().Values<string>("name").Bytecode);
        
        Assert.Equal("g.V().values('name')", translated);
    }
    
    [Fact]
    public void ShouldTranslateStepsWithMultipleArguments()
    {
        var translator = CreateTranslator();

        var translated = translator.Translate(_g.V().Values<string>("name", "age").Bytecode);
        
        Assert.Equal("g.V().values('name', 'age')", translated);
    }

    [Fact]
    public void ShouldTranslateNullArgument()
    {
        AssertTranslation("null", null);
    }
    
    [Theory]
    [InlineData("3, 5", 3, 5)]
    [InlineData("3.2, 5.1", 3.2, 5.1)]
    [InlineData("'c'", 'c')]
    public void ShouldTranslateSimpleTypes(string expectedGroovy, params object[] simpleTypes)
    {
        AssertTranslation(expectedGroovy, simpleTypes);
    }

    [Fact]
    public void ShouldTranslateDateTimeOffsetArgument()
    {
        AssertTranslation("new Date(122, 11, 30, 12, 0, 1)", DateTimeOffset.Parse("2022-12-30T12:00:01Z"));
    }

    [Fact]
    public void ShouldTranslateGuid()
    {
        AssertTranslation("UUID.fromString('ffffffff-fd49-1e4b-0000-00000d4b8a1d')",
            Guid.Parse("ffffffff-fd49-1e4b-0000-00000d4b8a1d"));
    }

    [Fact]
    public void ShouldTranslateCollection()
    {
        AssertTranslation("['test1', 'test2']", new List<string>{"test1", "test2"});
    }

    [Fact]
    public void ShouldTranslateDictionary()
    {
        var dictionary = new Dictionary<object, object>
        {
            { "key1", "value1" },
            { 1, "value2" }
        };
        AssertTranslation("['key1': 'value1', 1: 'value2']", dictionary);
    }

    [Fact]
    public void ShouldTranslateColumn()
    {
        AssertTranslation("Column.keys", Column.Keys);
    }
    
    [Fact]
    public void ShouldTranslateDirection()
    {
        AssertTranslation("Direction.BOTH", Direction.Both);
    }
    
    [Fact]
    public void ShouldTranslateOrder()
    {
        AssertTranslation("Order.desc", Order.Desc);
    }
    
    [Fact]
    public void ShouldTranslatePop()
    {
        AssertTranslation("Pop.last", Pop.Last);
    }
    
    [Fact]
    public void ShouldTranslateScope()
    {
        AssertTranslation("Scope.local", Scope.Local);
    }

    [Fact]
    public void ShouldTranslateP()
    {
        AssertTranslation("P.and(P.gt(20), P.lt(30))", P.Gt(20).And(P.Lt(30)));
    }

    [Fact]
    public void ShouldTranslatePBetween()
    {
        AssertTranslation("P.between([20, 30])", P.Between(20, 30));
    }

    [Fact]
    public void DebugHelperTest()
    {
        AssertTraversalTranslation("g.V('3').repeat(__.out('route').simplePath()).times(2).path().by('code')",
            _g.V("3").Repeat(__.Out("route").SimplePath()).Times(2).Path().By("code"));
    }
    
    [Fact]
    public void TranslationTest()
    {
        var traversalByExpectedGroovyScript = new Dictionary<string, ITraversal>
        {
            { "g.V()", _g.V() },
            { "g.V('1', '2', '3', '4')", _g.V("1", "2", "3", "4") },
            { "g.V('3').valueMap(True)", _g.V("3").ValueMap<object, object>(true) },
            { "g.V().constant(5)", _g.V().Constant(5) },
            { "g.V().constant(1.5)", _g.V().Constant(1.5) },
            { "g.V().constant('Hello')", _g.V().Constant("Hello") },
            { "g.V().hasLabel('airport').limit(5)", _g.V().HasLabel("airport").Limit<Vertex>(5) },
            {
                "g.V().hasLabel(P.within(['a', 'b', 'c']))",
                _g.V().HasLabel(P.Within(new List<string> { "a", "b", "c" }))
            },
            {
                "g.V().hasLabel('airport', 'continent').out().limit(5)",
                _g.V().HasLabel("airport", "continent").Out().Limit<Vertex>(5)
            },
            {
                "g.V().hasLabel('airport').out().values('code').limit(5)",
                _g.V().HasLabel("airport").Out().Values<string>("code").Limit<Vertex>(5)
            },
            {
                "g.V('3').as('a').out('route').limit(10).where(P.eq('a')).by('region')",
                _g.V("3").As("a").Out("route").Limit<Vertex>(10).Where(P.Eq("a")).By("region")
            },
            {
                "g.V('3').repeat(__.out('route').simplePath()).times(2).path().by('code')",
                _g.V("3").Repeat(__.Out("route").SimplePath()).Times(2).Path().By("code")
            },
            {
                "g.V().hasLabel('airport').out().has('region', 'US-TX').values('code').limit(5)",
                _g.V().HasLabel("airport").Out().Has("region", "US-TX").Values<string>("code").Limit<string>(5)
            },
            {
                "g.V().hasLabel('airport').union(__.values('city'), __.values('region')).limit(5)",
                _g.V().HasLabel("airport").Union<string>(__.Values<string>("city"), __.Values<string>("region"))
                    .Limit<string>(5)
            },
            { "g.V('3').as('a').out('route', 'routes')", _g.V("3").As("a").Out("route", "routes") },
            { "g.V().where(__.values('runways').is(5))", _g.V().Where(__.Values<int>("runways").Is(5)) },
            {
                "g.V('3').repeat(__.out().simplePath()).until(__.has('code', 'AGR')).path().by('code').limit(5)",
                _g.V("3").Repeat(__.Out().SimplePath()).Until(__.Has("code", "AGR")).Path().By("code").Limit<Path>(5)
            },
            { "g.V().hasLabel('airport').order().by(__.id())", _g.V().HasLabel("airport").Order().By(__.Id()) },
            { "g.V().hasLabel('airport').order().by(T.id)", _g.V().HasLabel("airport").Order().By(T.Id) },
            {
                "g.V().hasLabel('airport').order().by(__.id(), Order.desc)",
                _g.V().HasLabel("airport").Order().By(__.Id(), Order.Desc)
            },
            {
                "g.V().hasLabel('airport').order().by('code', Order.desc)",
                _g.V().HasLabel("airport").Order().By("code", Order.Desc)
            },
            {
                "g.V('1', '2', '3').local(__.out().out().dedup().fold())",
                _g.V("1", "2", "3").Local<object>(__.Out().Out().Dedup().Fold())
            },
            { "g.V('3').out().path().count(Scope.local)", _g.V("3").Out().Path().Count(Scope.Local) },
            { "g.E().count()", _g.E().Count() },
            { "g.V('5').outE('route').inV().path().limit(10)", _g.V('5').OutE("route").InV().Path().Limit<Path>(10) },
            {
                "g.V('5').propertyMap().select(Column.keys)",
                _g.V('5').PropertyMap<object>().Select<object>(Column.Keys)
            },
            {
                "g.V('5').propertyMap().select(Column.values)",
                _g.V('5').PropertyMap<object>().Select<object>(Column.Values)
            },
            { "g.V('3').values('runways').math('_ + 1')", _g.V("3").Values<string>("runways").Math("_ + 1") },
            {
                "g.V('3').emit().repeat(__.out().simplePath()).times(3).limit(5).path()",
                _g.V("3").Emit().Repeat(__.Out().SimplePath()).Times(3).Limit<Vertex>(5).Path()
            },
            {
                "g.V().match(__.as('a').has('code', 'LHR').as('b')).select('b').by('code')",
                _g.V().Match<object>(__.As("a").Has("code", "LHR").As("b")).Select<object>("b").By("code")
            },
            {
                "g.V().has('test-using-keyword-as-property', 'repeat')",
                _g.V().Has("test-using-keyword-as-property", "repeat")
            },
            { "g.V('1').addE('test').to(__.V('4'))", _g.V('1').AddE("test").To(__.V('4')) },
            { "g.V().values('runways').max()", _g.V().Values<int>("runways").Max<int>() },
            { "g.V().values('runways').min()", _g.V().Values<int>("runways").Min<int>() },
            { "g.V().values('runways').sum()", _g.V().Values<int>("runways").Sum<int>() },
            { "g.V().values('runways').mean()", _g.V().Values<int>("runways").Mean<double>() },
            {
                "g.withSack(0).V('3', '5').sack(Operator.sum).by('runways').sack()",
                _g.WithSack(0).V('3', '5').Sack(Operator.Sum).By("runways").Sack<object>()
            },
            {
                "g.V('3').values('runways').store('x').V('4').values('runways').store('x').by(__.constant(1)).V('6').store('x').by(__.constant(1)).select('x').unfold().sum()",
                _g.V("3").Values<object>("runways").Store("x").V('4').Values<object>("runways").Store("x")
                    .By(__.Constant(1)).V('6').Store("x").By(__.Constant(1)).Select<object>("x").Unfold<object>()
                    .Sum<object>()
            },
            { "g.inject(3, 4, 5)", _g.Inject(3, 4, 5) },
            { "g.inject([3, 4, 5])", _g.Inject(new List<int> { 3, 4, 5 }) },
            { "g.inject(3, 4, 5).count()", _g.Inject(3, 4, 5).Count() },
            { "g.V().has('runways', P.gt(5)).count()", _g.V().Has("runways", P.Gt(5)).Count() },
            { "g.V().has('runways', P.lte(5.3)).count()", _g.V().Has("runways", P.Lte(5.3)).Count() },
            { "g.V().has('code', P.within([123, 124]))", _g.V().Has("code", P.Within(new List<int> { 123, 124 })) },
// {"g.V('1', '2').has('region', P.within('US-TX','US-GA'))", _g.V('1','2').has('region',within(['US-TX','US-GA']))},
// {"g.V().and_(__.has('runways', P.gt(5)), __.has('region', 'US-TX'))", _g.V().and(__.has('runways',gt(5)),__.has('region','US-TX'))},
// {"g.V().union(__.has('runways', gt(5)), __.has('region', 'US-TX'))", _g.V().union(__.has('runways',gt(5)),__.has('region','US-TX'))},
// {"g.V('3').choose(__.values('runways').is(3), __.constant('three'),__.constant('not three'))", _g.V("3").choose(__.values('runways').is(3),__.constant('three'),__.constant('not three'))},
// {"g.V('3').choose(__.values('runways')).option(1, __.constant('three')).option(2,__.constant('not three'))", _g.V("3").choose(__.values('runways')).option(1,__.constant('three')).option(2,__.constant('not three'))},
// {"g.V('3').choose(__.values('runways')).option(1.5, __.constant('one and a half')).option(2,__.constant('not three'))", _g.V("3").choose(__.values('runways')).option(1.5,__.constant('one and a half')).option(2,__.constant('not three'))},
// {"g.V('3').repeat(__.out().simplePath()).until(__.loops().is(1)).count()", _g.V("3").repeat(__.out().simplePath()).until(__.loops().is(1)).count()},
// {"g.V().hasLabel('airport').limit(20).group().by('region').by('code').order(Scope.local).by(Column.keys)", _g.V().HasLabel("airport").limit(20).group().by('region').by('code').order(Scope.local).by(Column.keys)},
// {"g.V('1').as('a').V('2').as('a').select(Pop.all_, 'a')", _g.V('1').as('a').V('2').as('a').select(Pop.all,'a')},
// {"g.addV('test').property(Cardinality.set_, 'p1', 10)", _g.AddV("test").property(Cardinality.set,'p1',10)},
// {"g.addV('test').property(Cardinality.list_, 'p1', 10)", _g.AddV("test").property(Cardinality.list,'p1',10)},
//
// {"g.addV('test').property(Cardinality.single, 'p1', 10)", _g.AddV("test").property(Cardinality.single,'p1',10)},
// {"g.V().limit(5).order().by(T.label)", _g.V().limit(5).order().by(T.label)},
//
// {"g.V().range_(1, 5)", _g.V().range(1,5)},
//
// {"g.addV('test').property('p1', 123)", _g.AddV("test").property('p1',123)},
//
// {"g.addV('test').property('date',datetime(2021, 2, 1, 9, 30))", _g.AddV("test").property('date',new Date(121,2,1,9,30,0))},
// {"g.addV('test').property('date',datetime(2021, 2, 1))", _g.AddV("test").property('date',new Date(121,2,1,0,0,0))},
// {"g.addE('route').from_(__.V('1')).to(__.V('2'))", _g.addE('route').from(__.V('1')).to(__.V('2'))},
// {"g.withSideEffect('a', [1, 2]).V('3').select('a')", _g.withSideEffect('a',[1, 2]).V('3').select('a')},
// {"g.withSideEffect('a', 1).V('3').select('a')", _g.withSideEffect('a',1).V('3').select('a')},
// {"g.withSideEffect('a', 'abc').V('3').select('a')", _g.withSideEffect('a','abc').V('3').select('a')},
// {"g.V().has('airport', 'region', 'US-NM').limit(3).values('elev').fold().index()", _g.V().Has('airport','region','US-NM').limit(3).values('elev').fold().index()},
// {"g.V('3').repeat(__.timeLimit(1000).out().simplePath()).until(__.has('code', 'AGR')).path()", _g.V("3").repeat(__.timeLimit(1000).out().simplePath()).until(__.has('code','AGR')).path()},
//
// {"g.V().hasLabel('airport').where(__.values('elev').is(gt(14000)))", _g.V().HasLabel("airport").where(__.values('elev').is(gt(14000)))},
//
// {"g.V().hasLabel('airport').where(__.out().count().is(gt(250))).values('code')", _g.V().HasLabel("airport").where(__.out().count().is(gt(250))).values('code')},
//
// {"g.V().hasLabel('airport').filter_(__.out().count().is(gt(250))).values('code')", _g.V().HasLabel("airport").filter(__.out().count().is(gt(250))).values('code')},
// {"g.withSack(0).
//                         V('3').
//                         repeat(__.outE('route').sack(Operator.sum).by('dist').inV()).
//                         until(__.has('code', 'AGR').or_().loops().is(4)).
//                         has('code', 'AGR').
//                         local(__.union(__.path().by('code').by('dist'),__.sack()).fold()).
//                         limit(10)", _g.withSack(0).V('3').repeat(__.outE('route').sack(Operator.sum).by('dist').inV()).until(__.has('code','AGR').or().loops().is(4)).has('code','AGR').local(__.union(__.path().by('code').by('dist'),__.sack()).fold()).limit(10)},
//
// {"g.addV().as('a').addV().as('b').addE('knows').from_('a').to('b')", _g.AddV().as('a').addV().as('b').addE('knows').from('a').to('b')},
//
// {"g.addV('Person').as('a').addV('Person').as('b').addE('knows').from_('a').to('b')", _g.AddV('Person').as('a').addV('Person').as('b').addE('knows').from('a').to('b')},
// {"g.V('3').project('Out','In').by(__.out().count()).by(__.in_().count())", _g.V("3").project('Out','In').by(__.out().count()).by(__.in().count())},
// {"g.V('44').out().aggregate('a').out().where(within('a')).path()", _g.V('44').out().aggregate('a').out().where(within(['a'])).path()},
// {"g.V().has('date', datetime(2021, 2, 22))", _g.V().Has('date',new Date(121,2,22,0,0,0))},
// {"g.V().has('date', within(datetime(2021, 2, 22), datetime(2021, 1, 1)))",  _g.V().Has('date',within([new Date(121,2,22,0,0,0),new Date(121,1,1,0,0,0)]))},
// {"g.V().has('date', between(datetime(2021, 1, 1), datetime(2021, 2, 22)))",            _g.V().Has('date',between(new Date(121,1,1,0,0,0),new Date(121,2,22,0,0,0)))},
// {"g.V().has('date', inside(datetime(2021, 1, 1),datetime(2021, 2, 22)))",            _g.V().Has('date',inside(new Date(121,1,1,0,0,0),new Date(121,2,22,0,0,0)))},
// {"g.V().has('date', P.gt(datetime(2021, 1, 1, 9, 30)))", _g.V().Has('date',gt(new Date(121,1,1,9,30,0)))},
// {"g.V().has('runways', between(3,5))", _g.V().Has('runways',between(3,5))},
// {"g.V().has('runways', inside(3,5))", _g.V().Has('runways',inside(3,5))},
// {"g.V('44').outE().elementMap()", _g.V('44').outE().elementMap()},
// {"g.V('44').valueMap().by(__.unfold())", _g.V('44').valueMap().by(__.unfold())},
// {"g.V('44').valueMap().with_(WithOptions.tokens,WithOptions.labels)", _g.V('44').valueMap().with(WithOptions.tokens,WithOptions.labels)},
// {"g.V('44').valueMap().with_(WithOptions.tokens)", _g.V('44').valueMap().with(WithOptions.tokens)},
// {"g.withStrategies(ReadOnlyStrategy()).addV('test')",  _g.withStrategies(new ReadOnlyStrategy()).addV('test')},
//         strategy = SubgraphStrategy(vertices=__.has('region', 'US-TX'), edges=__.hasLabel('route'))
// {"g.withStrategies(strategy).V().count()", _g.withStrategies(new SubgraphStrategy(vertices:__.has('region','US-TX'),edges:__.hasLabel('route'))).V().count()},
//         strategy = SubgraphStrategy(vertex_properties=__.hasNot('runways'))
// {"g.withStrategies(strategy).V().count()",  _g.withStrategies(new SubgraphStrategy(vertexProperties:__.hasNot('runways'))).V().count()},
//         strategy = SubgraphStrategy(vertices=__.has('region', 'US-TX'), vertex_properties=__.hasNot('runways'))
// {"g.withStrategies(strategy).V().count()",  _g.withStrategies(new SubgraphStrategy(vertices:__.has('region','US-TX'),vertexProperties:__.hasNot('runways'))).V().count()},
//         strategy = SubgraphStrategy(vertices=__.has('region', 'US-TX'), edges=__.hasLabel('route'))
            {
                "g.withStrategies(ReadOnlyStrategy(),strategy).V().count()",
                _g.WithStrategies(new ReadOnlyStrategy(),
                    new SubgraphStrategy(vertices: __.Has("region", "US-TX"), edges: __.HasLabel("route"))).V().Count()
            },
//         strategy = SubgraphStrategy(vertices=__.has('region', 'US-TX'))
// {"g.withStrategies(ReadOnlyStrategy(), strategy).V().count()",  _g.withStrategies(new ReadOnlyStrategy(),new SubgraphStrategy(vertices:__.has('region','US-TX'))).V().count()},
// {"g.with_('evaluationTimeout', 500).V().count()",  _g.withStrategies(new OptionsStrategy(evaluationTimeout:500)).V().count()},
            {
                "g.withStrategies(OptionsStrategy({'evaluationTimeout': 500})).V().count()",
                _g.WithStrategies(new OptionsStrategy(new Dictionary<string, object> { { "evaluationTimeout", 500 } }))
                    .V().Count()
            },
// {"g.withStrategies(PartitionStrategy(partition_key="partition", write_partition="a", read_partitions=["a},).addV('test')", _g.withStrategies(new PartitionStrategy(partitionKey:'partition',writePartition:'a',readPartitions:['a'])).addV('test')},
// {"g.withComputer().V().shortestPath().with_(ShortestPath.target, __.has('name','peter'))", _g.withStrategies(new VertexProgramStrategy()).V().shortestPath().with('~tinkerpop.shortestPath.target',__.has('name','peter'))}
        };

        foreach (var (expectedGroovyScript, traversal) in traversalByExpectedGroovyScript)
        {
            AssertTraversalTranslation(expectedGroovyScript, traversal);
        }
    }

    private void AssertTranslation(string expectedTranslation, params object[] objs)
    {
        AssertTraversalTranslation($"g.inject({expectedTranslation})", _g.Inject(objs));
    }
    
    private static void AssertTraversalTranslation(string expectedTranslation, ITraversal traversal)
    {
        var translator = CreateTranslator();
        
        var translated = translator.Translate(traversal.Bytecode);
        
        Assert.Equal(expectedTranslation, translated);
    }

    private static GroovyTranslator CreateTranslator() => GroovyTranslator.Of("g");
}
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
using Gremlin.Net.Process.Traversal.Translator;
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
    public void ShouldTranslateNumericArgument(string numericGroovyString, params object[] numbers)
    {
        AssertTranslation(numericGroovyString, numbers);
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

    private void AssertTranslation(string expectedTranslation, params object[] objs)
    {
        var translator = CreateTranslator();
        
        var translated = translator.Translate(_g.Inject(objs).Bytecode);
        
        Assert.Equal($"g.inject({expectedTranslation})", translated);
    }

    private static GroovyTranslator CreateTranslator() => GroovyTranslator.Of("g");
}
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
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;

namespace Gremlin.Net.Process.Traversal.Translator
{
    /// <summary>
    ///     Converts bytecode to a Groovy string of Gremlin.
    /// </summary>
    public class GroovyTranslator
    {
        private GroovyTranslator(string traversalSource)
        {
            TraversalSource = traversalSource;
        }

        /// <summary>
        ///     Creates the translator.
        /// </summary>
        /// <param name="traversalSource">The traversal source for the traversal to be translated.</param>
        /// <returns>The created translator instance.</returns>
        public static GroovyTranslator Of(string traversalSource)
        {
            return new GroovyTranslator(traversalSource);
        }

        /// <summary>
        ///     Get the language that the translator is converting the traversal byte code to.
        /// </summary>
        public string TargetLanguage => "gremlin-groovy";

        /// <summary>
        ///     Gets the <see cref="TraversalSource"/> representation rooting this translator.
        ///     This is typically a "g".
        /// </summary>
        public string TraversalSource { get; }

        /// <summary>
        ///     Translate <see cref="Bytecode"/> into gremlin-groovy.
        /// </summary>
        /// <param name="bytecode">The bytecode representing traversal source and traversal manipulations.</param>
        /// <param name="isChildTraversal">Whether this is an anonymous traversal (started via '__').</param>
        /// <returns>The translated gremlin-groovy traversal.</returns>
        public string Translate(Bytecode bytecode, bool isChildTraversal = false)
        {
            var sb = new StringBuilder(isChildTraversal ? "__" : TraversalSource);
            
            foreach (var step in bytecode.SourceInstructions)
            {
                sb.Append(TranslateStep(step));
            }
            
            foreach (var step in bytecode.StepInstructions)
            {
                sb.Append(TranslateStep(step));
            }

            return sb.ToString();
        }

        private string TranslateStep(Instruction step)
        {
            return $".{step.OperatorName}({TranslateArguments(step.Arguments)})";
        }

        private string TranslateArguments(IReadOnlyCollection<object> arguments)
        {
            var argumentsAsStrings = new List<string>(arguments.Count);

            foreach (var argument in arguments)
            {
                argumentsAsStrings.Add(TranslateArgument(argument));
            }

            return string.Join(", ", argumentsAsStrings);
        }

        private string TranslateArgument(object argument)
        {
            if (argument == null)
                return "null";
            if (argument is string str)
                return $"'{str}'";
            if (argument is char c)
                return $"'{c}'";
            if (argument is DateTimeOffset dto)
                return TranslateDateTimeOffset(dto);
            if (argument is Guid guid)
                return TranslateGuid(guid);
            if (argument is P p)
                return TranslateP(p);
            if (argument is IDictionary dict)
            {
                return TranslateDictionary(dict);
            }
            if (argument is IEnumerable e)
            {
                return TranslateCollection(e);
            }

            if (argument is ITraversal t)
            {
                return TranslateTraversal(t);
            }
            return Convert.ToString(argument, CultureInfo.InvariantCulture);
        }

        private string TranslateTraversal(ITraversal traversal)
        {
            return Translate(traversal.Bytecode, true);
        }

        private string TranslateDictionary(IDictionary dict)
        {
            var kvStrings = new List<string>(dict.Count);
            foreach (DictionaryEntry kv in dict)
            {
                kvStrings.Add($"{TranslateArgument(kv.Key)}: {TranslateArgument(kv.Value)}");
            }

            return $"[{string.Join(", ", kvStrings)}]";
        }

        private bool IsDictionaryType(Type type)
        {
            return type
                .GetInterfaces()
                .Any(interfaceType => interfaceType.IsConstructedGenericType
                                      && interfaceType.GetGenericTypeDefinition() == typeof(IEnumerable<>) 
                                      && interfaceType.GenericTypeArguments[0] is { IsConstructedGenericType: true } genericArgument 
                                      && genericArgument.GetGenericTypeDefinition() == typeof(KeyValuePair<,>));
        }

        private string TranslateCollection(IEnumerable enumerable) =>
            $"[{TranslateArguments(enumerable.Cast<object>().ToArray())}]";

        private string TranslateP(P p)
        {
            return p.Other == null
                ? $"P.{p.OperatorName}({TranslateArgument(p.Value)})"
                : $"P.{p.OperatorName}({TranslateArgument(p.Value)}, {TranslateArgument(p.Other)})";
        }

        private static string TranslateGuid(Guid guid) => $"UUID.fromString('{guid}')";

        private static string TranslateDateTimeOffset(DateTimeOffset dto)
        {
            var year = dto.Year - 1900;
            var month = dto.Month - 1;
            var dayOfMonth = dto.Day;
            var hour = dto.Hour;
            var minute = dto.Minute;
            var second = dto.Second;
            return $"new Date({year}, {month}, {dayOfMonth}, {hour}, {minute}, {second})";
        }
    }
}
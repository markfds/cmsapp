/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.performance.fixture;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.gradle.internal.ErroringAction;
import org.gradle.internal.IoActions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simplifies stacks to make flame graphs more readable.
 */
class FlameGraphSanitizer {
    private static final Splitter STACKTRACE_SPLITTER = Splitter.on(";").omitEmptyStrings();
    private static final Joiner STACKTRACE_JOINER = Joiner.on(";");

    private final SanitizeFunction sanitizeFunction;

    public FlameGraphSanitizer(SanitizeFunction... sanitizeFunctions) {
        this.sanitizeFunction = new CompositeSanitizeFunction(sanitizeFunctions);
    }

    public void sanitize(final File in, File out) {
        IoActions.writeTextFile(out, new ErroringAction<BufferedWriter>() {
            @Override
            protected void doExecute(BufferedWriter writer) throws Exception {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(in)));
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    int endOfStack = line.lastIndexOf(' ');
                    if (endOfStack > 0) {
                        String stackTrace = line.substring(0, endOfStack);
                        String invocationCount = line.substring(endOfStack + 1);
                        List<String> stackTraceElements = STACKTRACE_SPLITTER.splitToList(stackTrace);
                        List<String> sanitizedStackElements = new ArrayList<String>(stackTraceElements.size());
                        for (String stackTraceElement : stackTraceElements) {
                            String sanitizedStackElement = sanitizeFunction.map(stackTraceElement);
                            if (sanitizedStackElement != null) {
                                String previousStackElement = Iterables.getLast(sanitizedStackElements, null);
                                if (!sanitizedStackElement.equals(previousStackElement)) {
                                    sanitizedStackElements.add(sanitizedStackElement);
                                }
                            }
                        }
                        if (!sanitizedStackElements.isEmpty()) {
                            sb.setLength(0);
                            STACKTRACE_JOINER.appendTo(sb, sanitizedStackElements);
                            sb.append(' ');
                            sb.append(invocationCount);
                            sb.append("\n");
                            writer.write(sb.toString());
                        }
                    }
                }
            }
        });
    }

    public interface SanitizeFunction {
        SanitizeFunction COLLAPSE_BUILD_SCRIPTS = new FlameGraphSanitizer.RegexBasedSanitizeFunction(
            ImmutableMap.of(
                Pattern.compile("build_[a-z0-9]+"), "build script",
                Pattern.compile("settings_[a-z0-9]+"), "settings script"
            )
        );

        String map(String entry);
    }

    private static class CompositeSanitizeFunction implements SanitizeFunction {

        private final List<SanitizeFunction> sanitizeFunctions;

        private CompositeSanitizeFunction(SanitizeFunction... sanitizeFunctions) {
            this.sanitizeFunctions = ImmutableList.copyOf(sanitizeFunctions);
        }

        @Override
        public String map(String entry) {
            String result = entry;
            for (SanitizeFunction sanitizeFunction : sanitizeFunctions) {
                result = sanitizeFunction.map(result);
            }
            return result;
        }
    }

    public static class ContainmentBasedSanitizeFunction implements SanitizeFunction {
        private final Map<String, String> replacements;

        public ContainmentBasedSanitizeFunction(Map<List<String>, String> replacements) {
            this.replacements = Maps.newHashMap();
            for (Map.Entry<List<String>, String> entry : replacements.entrySet()) {
                for (String key : entry.getKey()) {
                    this.replacements.put(key, entry.getValue());
                }
            }
        }

        @Override
        public String map(String entry) {
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                if (entry.contains(replacement.getKey())) {
                    return replacement.getValue();
                }
            }
            return entry;
        }
    }

    public static class RegexBasedSanitizeFunction implements SanitizeFunction {
        private final Map<Pattern, String> replacements;

        public RegexBasedSanitizeFunction(Map<Pattern, String> replacements) {
            this.replacements = replacements;
        }

        @Override
        public String map(String entry) {
            for (Map.Entry<Pattern, String> replacement : replacements.entrySet()) {
                Matcher matcher = replacement.getKey().matcher(entry);
                String value = replacement.getValue();
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    matcher.appendReplacement(sb, value);
                }
                matcher.appendTail(sb);
                if (sb.length() > 0) {
                    entry = sb.toString();
                }
            }
            return entry;
        }

    }
}

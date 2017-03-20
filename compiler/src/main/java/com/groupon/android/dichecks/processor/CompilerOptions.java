/*
 * Copyright (c) 2017, Groupon, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groupon.android.dichecks.processor;

/** Compiler options for DiChecksProcessor. */
//This design doesn't scale but we don't expect much more than a few checks, if that changes we should consider other implementations.
public interface CompilerOptions {
  String OPTIONS_PREFIX = "com.groupon.android.dichecks.";
  /** Enables/Disables the duplicate check. */
  String DUPLICATE_INJECTION_IN_HIERARCHY_ENABLED = OPTIONS_PREFIX + "duplicateCheck.enabled";
  /** Whether or not the duplicate check fails the builds when an issue is detected. */
  String DUPLICATE_INJECTION_IN_HIERARCHY_FAIL_ON_ERROR =
      OPTIONS_PREFIX + "duplicateCheck.failOnError";

  /** Enables/Disables the forbidden classes check. */
  String FORBIDDEN_CLASSES_ENABLED = OPTIONS_PREFIX + "forbiddenInjectClassesCheck.enabled";
  /** Whether or not the forbidden classes check fails the builds when an issue is detected. */
  String FORBIDDEN_CLASSES_FAIL_ON_ERROR =
      OPTIONS_PREFIX + "forbiddenInjectClassesCheck.failOnError";
  /** Comma separated list of forbidden classes. */
  String FORBIDDEN_CLASSES_CLASSLIST =
      OPTIONS_PREFIX + "forbiddenInjectClassesCheck.forbiddenInjectedClasses";
}

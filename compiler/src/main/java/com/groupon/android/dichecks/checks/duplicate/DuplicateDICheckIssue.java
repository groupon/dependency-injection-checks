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

package com.groupon.android.dichecks.checks.duplicate;

import com.groupon.android.dichecks.checks.common.DICheckIssue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import org.jetbrains.annotations.NotNull;

public class DuplicateDICheckIssue extends DICheckIssue {

  private static final String MESSAGE_FORMAT =
      "Duplicate injection found: injected class %1$s in %2$s also found in %3$s.";

  public DuplicateDICheckIssue(
      @NotNull Diagnostic.Kind type,
      @NotNull Element element,
      @NotNull TypeElement enclosingClass,
      @NotNull TypeElement duplicateClass) {
    super(
        type,
        String.format(
            MESSAGE_FORMAT,
            element.asType().toString(),
            enclosingClass.getQualifiedName().toString(),
            duplicateClass.getQualifiedName().toString()),
        element);
  }
}

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

import static com.groupon.android.dichecks.processor.CompilerOptions.DUPLICATE_INJECTION_IN_HIERARCHY_ENABLED;
import static com.groupon.android.dichecks.processor.CompilerOptions.DUPLICATE_INJECTION_IN_HIERARCHY_FAIL_ON_ERROR;
import static com.groupon.android.dichecks.processor.CompilerOptions.FORBIDDEN_CLASSES_CLASSLIST;
import static com.groupon.android.dichecks.processor.CompilerOptions.FORBIDDEN_CLASSES_ENABLED;
import static com.groupon.android.dichecks.processor.CompilerOptions.FORBIDDEN_CLASSES_FAIL_ON_ERROR;
import static javax.lang.model.SourceVersion.RELEASE_7;

import com.google.auto.service.AutoService;
import com.groupon.android.dichecks.checks.common.DICheck;
import com.groupon.android.dichecks.checks.common.DICheckIssue;
import com.groupon.android.dichecks.checks.duplicate.DuplicateInjectionInHierarchyCheck;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/** Main entry class of the annotation processor used in dependency injection checks */
@AutoService(Processor.class)
@SupportedOptions(
  value = {
    DUPLICATE_INJECTION_IN_HIERARCHY_ENABLED,
    DUPLICATE_INJECTION_IN_HIERARCHY_FAIL_ON_ERROR,
    FORBIDDEN_CLASSES_ENABLED,
    FORBIDDEN_CLASSES_FAIL_ON_ERROR,
    FORBIDDEN_CLASSES_CLASSLIST
  }
)
@SupportedAnnotationTypes(value = {DiChecksProcessor.INJECT_ANNOTATION_CLASSNAME})
@SupportedSourceVersion(RELEASE_7)
public class DiChecksProcessor extends AbstractProcessor {

  public static final String CLASS_LIST_SEPARATOR = ",";
  public static final String INJECT_ANNOTATION_CLASSNAME = "javax.inject.Inject";

  // compiler argument values
  private boolean duplicateInjectionInHierarchyEnabled = true;
  private boolean duplicateInjectionInHierarchyFailOnError = true;
  private boolean forbiddenClassesEnabled = true;
  private boolean forbiddenClassesFailOnError = true;
  private String[] forbiddenClassesClasses;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    final long startTimeMillis = System.currentTimeMillis();
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "starting DI checks");

    initializeCompilerArguments();

    final List<DICheck> checks = buildDICheckList();

    // annotations passed as a parameter in this method only refers to TypeElements
    final Set<? extends Element> annotatedElements =
        roundEnv.getElementsAnnotatedWith(Inject.class);

    final Set<VariableElement> injectedFields = ElementFilter.fieldsIn(annotatedElements);
    final List<DICheckIssue> issuesFound = new ArrayList<>();

    for (DICheck check : checks) {
      check.addInjectedElements(injectedFields);
      issuesFound.addAll(check.processInjectedElements());
    }

    processingEnv
        .getMessager()
        .printMessage(
            Diagnostic.Kind.NOTE,
            String.format("DI checks took %dms", System.currentTimeMillis() - startTimeMillis));

    for (DICheckIssue issue : issuesFound) {
      processingEnv.getMessager().printMessage(issue.getKind(), issue.getMsg(), issue.getElement());
    }

    return false;
  }

  private List<DICheck> buildDICheckList() {
    final List<DICheck> checks = new ArrayList<>();

    if (duplicateInjectionInHierarchyEnabled) {
      checks.add(
          new DuplicateInjectionInHierarchyCheck(
              processingEnv, duplicateInjectionInHierarchyFailOnError));
    }

    return checks;
  }

  private void initializeCompilerArguments() {
    duplicateInjectionInHierarchyEnabled =
        readCompilerFlag(
            DUPLICATE_INJECTION_IN_HIERARCHY_ENABLED, duplicateInjectionInHierarchyEnabled);
    duplicateInjectionInHierarchyFailOnError =
        readCompilerFlag(
            DUPLICATE_INJECTION_IN_HIERARCHY_FAIL_ON_ERROR,
            duplicateInjectionInHierarchyFailOnError);
    forbiddenClassesEnabled = readCompilerFlag(FORBIDDEN_CLASSES_ENABLED, forbiddenClassesEnabled);
    forbiddenClassesFailOnError =
        readCompilerFlag(FORBIDDEN_CLASSES_FAIL_ON_ERROR, forbiddenClassesFailOnError);
    forbiddenClassesClasses =
        readCompilerStringArray(
            FORBIDDEN_CLASSES_CLASSLIST, CLASS_LIST_SEPARATOR, forbiddenClassesClasses);
  }

  private String[] readCompilerStringArray(
      String argumentName, String separator, String[] defaultValue) {
    final Map<String, String> options = processingEnv.getOptions();
    if (options.containsKey(argumentName)) {
      return options.get(argumentName).split(separator);
    }
    return defaultValue;
  }

  private boolean readCompilerFlag(String flagName, boolean defaultValue) {
    final Map<String, String> options = processingEnv.getOptions();
    if (options.containsKey(flagName)) {
      return Boolean.parseBoolean(options.get(flagName));
    }
    return defaultValue;
  }

  public void setDuplicateInjectionInHierarchyEnabled(
      boolean duplicateInjectionInHierarchyEnabled) {
    this.duplicateInjectionInHierarchyEnabled = duplicateInjectionInHierarchyEnabled;
  }

  public void setDuplicateInjectionInHierarchyFailOnError(
      boolean duplicateInjectionInHierarchyFailOnError) {
    this.duplicateInjectionInHierarchyFailOnError = duplicateInjectionInHierarchyFailOnError;
  }

  public void setForbiddenClassesEnabled(boolean forbiddenClassesEnabled) {
    this.forbiddenClassesEnabled = forbiddenClassesEnabled;
  }

  public void setForbiddenClassesFailOnError(boolean forbiddenClassesFailOnError) {
    this.forbiddenClassesFailOnError = forbiddenClassesFailOnError;
  }

  public void setForbiddenClassesClasses(String[] forbiddenClassesClasses) {
    this.forbiddenClassesClasses = forbiddenClassesClasses;
  }
}

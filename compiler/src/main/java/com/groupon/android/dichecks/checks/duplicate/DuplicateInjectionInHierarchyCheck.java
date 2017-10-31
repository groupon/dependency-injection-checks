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

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

import com.groupon.android.dichecks.checks.common.DICheck;
import com.groupon.android.dichecks.checks.common.DICheckIssue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Named;
import javax.inject.Provider;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Use this to detect duplicate injections in the class hierarchy. */
public class DuplicateInjectionInHierarchyCheck implements DICheck {

  private static final String LAZY_CLASS_NAME = "Lazy";
  private Map<InjectionDefinition, Set<Element>> mapInjectionDefinitionToInjectionLocations =
      new HashMap<>();

  private final Elements elementUtils;
  private final Types typeUtils;

  private boolean failOnError;

  public DuplicateInjectionInHierarchyCheck(
      ProcessingEnvironment processingEnv, boolean failOnError) {
    this.failOnError = failOnError;
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
  }

  @Override
  public void addInjectedElements(Set<? extends Element> injectedElements) {
    for (Element injectedElement : injectedElements) {
      if (isProvider(injectedElement) || isLazy(injectedElement)) {
        final TypeElement typeElement = getKindParameter(injectedElement);
        if (typeElement != null) {
          addInjectionDefinition(new InjectionDefinition(typeElement), injectedElement);
        }
      } else {
        addInjectionDefinition(new InjectionDefinition(injectedElement), injectedElement);
      }
    }
  }

  /**
   * Main entry point to check for duplicates.
   *
   * @return all duplicate errors found based on the supplied injection definitions.
   * @see #addInjectionDefinition(InjectionDefinition, Element)
   * @see InjectionDefinition
   */
  /*
   * The current algorithm that checks for duplicates is to build up a map of (InjectionDefinition, set of containing classes). <br />
   * To collect the duplicates we iterate through this map and for each injection we check the set to see if any of the classes in the set has a superclass that's also in the set. <br />
   * This algorithm is not the most efficient in the sense that we may iterate through the same hierarchy multiple times for multiple InjectionDefinitions, <br />
   * however, after a simple test on a production codebase that uses injection heavily, this method runs in about 8ms (optimization is not critical). <br /><br />
   *
   * A better approach would be to implement a pseudo graph of the inheritance containing injections and apply a traversal function that calculates duplicates more efficiently <br />
   * (add up injection count of sub-graph for each InjectionDefinition, if injection count > 1 there's a duplicate). <br />
   * This could also make a more extensible framework to build up other checks.
   *
   */
  @Override
  public List<DICheckIssue> processInjectedElements() {
    final List<DICheckIssue> issues = new ArrayList<>();
    for (Map.Entry<InjectionDefinition, Set<Element>> entry :
        mapInjectionDefinitionToInjectionLocations.entrySet()) {
      final Set<Element> injectionLocations = entry.getValue();
      for (Element injectionLocation : injectionLocations) {
        final TypeElement ancestor = (TypeElement) injectionLocation.getEnclosingElement();
        TypeElement currentAncestor = ancestor;
        while (currentAncestor != null) {
          currentAncestor = findSuperClass(currentAncestor);
          if (isAncestorInLocations(currentAncestor, injectionLocations)) {
            issues.add(
                new DuplicateDICheckIssue(
                    failOnError ? ERROR : WARNING, injectionLocation, ancestor, currentAncestor));
          }
        }
      }
    }
    return issues;
  }

  /**
   * Builds up the internal data structures to make the detection of duplicates.
   *
   * @param injectionDefinition the injection definition (className + [named annotation]).
   */
  private void addInjectionDefinition(InjectionDefinition injectionDefinition, Element element) {
    Set<Element> injectionLocations =
        mapInjectionDefinitionToInjectionLocations.get(injectionDefinition);
    if (injectionLocations == null) {
      injectionLocations = new HashSet<>();
      mapInjectionDefinitionToInjectionLocations.put(injectionDefinition, injectionLocations);
    }

    injectionLocations.add(element);
  }

  private boolean isAncestorInLocations(TypeElement ancestor, Set<Element> injectionLocations) {
    for (Element injectionLocation : injectionLocations) {
      if (injectionLocation.getEnclosingElement() == ancestor) {
        return true;
      }
    }
    return false;
  }

  private TypeElement findSuperClass(TypeElement typeElement) {
    return (TypeElement) typeUtils.asElement(typeElement.getSuperclass());
  }

  /**
   * Tests whether one type is a subtype of another. Any type is considered to be a subtype of
   * itself.
   *
   * @param typeMirror1 the first type
   * @param typeMirror2 the second type
   * @return {@code true} if and only if the first type is a subtype of the second
   */
  private boolean isSubType(TypeMirror typeMirror1, TypeMirror typeMirror2) {
    return typeUtils.isSubtype(typeUtils.erasure(typeMirror1), typeUtils.erasure(typeMirror2));
  }

  private boolean isProvider(Element element) {
    return isSubType(
        element.asType(), elementUtils.getTypeElement(Provider.class.getCanonicalName()).asType());
  }

  private boolean isLazy(Element element) {
    final TypeMirror type = element.asType();
    if (type.getKind() == TypeKind.DECLARED) {
      final Name className = ((DeclaredType) type).asElement().getSimpleName();
      return LAZY_CLASS_NAME.equals(className.toString());
    }
    return false;
  }

  /**
   * Determine and return the type of the element.
   *
   * <ul>
   *   <li>Return erasure type Foo for, Lazy&lt;Foo&gt;.
   *   <li>Return FooProvider if no erasure type.
   *   <li>Return null, if we are unable to determine any type/kind of element.
   * </ul>
   *
   * @param element
   * @return the TypeElement for declared type.
   */
  @Nullable
  private TypeElement getKindParameter(Element element) {
    final TypeMirror type = element.asType();
    if (TypeKind.DECLARED == type.getKind()) {
      final List<? extends TypeMirror> typeMirrors = ((DeclaredType) type).getTypeArguments();
      if (typeMirrors.size() == 1) {
        return (TypeElement) typeUtils.asElement(typeUtils.erasure(typeMirrors.get(0)));
      } else {
        return (TypeElement) typeUtils.asElement(element.asType());
      }
    }
    return null;
  }

  /** Identifies an injection in the source code. */
  private static final class InjectionDefinition {

    private final String injectionType;
    private final String named;

    public InjectionDefinition(@NotNull Element element) {
      injectionType = element.asType().toString();
      final Named named = element.getAnnotation(Named.class);
      this.named = (named != null) ? named.value() : null;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof InjectionDefinition)) {
        return false;
      }
      final InjectionDefinition other = (InjectionDefinition) obj;
      return injectionType.equals(other.injectionType)
          && (named == null && other.named == null || named != null && named.equals(other.named));
    }

    @Override
    public int hashCode() {
      return injectionType.hashCode() * 31 + (named != null ? named.hashCode() : 0);
    }

    @Override
    public String toString() {
      return injectionType + (named != null ? "(named='" + named + "\')" : "");
    }
  }
}

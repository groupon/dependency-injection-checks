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

package com.groupon.android.dichecks;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.testing.compile.JavaFileObjects;
import com.groupon.android.dichecks.processor.DiChecksProcessor;
import javax.tools.JavaFileObject;
import org.junit.Test;

public class ProcessorTest {

  @Test
  public void testIntegration() {
    final JavaFileObject source =
        JavaFileObjects.forSourceString(
            "com.groupon.android.dichecks.dummy.A",
            Joiner.on('\n')
                .join(
                    "package com.groupon.android.dichecks.dummy;",
                    "import javax.inject.Inject;",
                    "import javax.inject.Named;",
                    "public class A {",
                    "    @Inject @Named(\"someNamedString\") String something;",
                    "    @Inject InjectedClass aClass;",
                    "}",
                    "class B extends A {}",
                    "class InjectedClass {}"));

    assertAbout(javaSource())
        .that(source)
        .processedWith(new DiChecksProcessor())
        .compilesWithoutError();
  }

  @Test
  public void compilationShouldFailIfDuplicateInjectionFound() {
    final JavaFileObject source =
        JavaFileObjects.forSourceString(
            "com.groupon.android.dichecks.dummy.A",
            Joiner.on('\n')
                .join(
                    "package com.groupon.android.dichecks.dummy;",
                    "import javax.inject.Inject;",
                    "import javax.inject.Named;",
                    "public class A {",
                    "    @Inject @Named(\"someNamedString\") String something;",
                    "    @Inject InjectedClass aClass;",
                    "}",
                    "class B extends A {",
                    "    @Inject InjectedClass aClass;",
                    "}",
                    "class InjectedClass {}"));

    assertAbout(javaSource())
        .that(source)
        .processedWith(new DiChecksProcessor())
        .failsToCompile()
        .withErrorContaining("Duplicate injection found: injected class "
                                 + "com.groupon.android.dichecks.dummy.InjectedClass in "
                                 + "com.groupon.android.dichecks.dummy.B also found in "
                                 + "com.groupon.android.dichecks.dummy.A.");
  }

  @Test
  public void compilationShouldNotFailIfDuplicateFoundAndWarningFlagSet() {
    final JavaFileObject source =
        JavaFileObjects.forSourceString(
            "com.groupon.android.dichecks.dummy.A",
            Joiner.on('\n')
                .join(
                    "package com.groupon.android.dichecks.dummy;",
                    "import javax.inject.Inject;",
                    "import javax.inject.Named;",
                    "public class A {",
                    "    @Inject @Named(\"someNamedString\") String something;",
                    "    @Inject InjectedClass aClass;",
                    "}",
                    "class B extends A {",
                    "    @Inject InjectedClass aClass;",
                    "}",
                    "class D extends B {}",
                    "class C extends A {}",
                    "class InjectedClass {}"));

    final DiChecksProcessor processor = new DiChecksProcessor();
    processor.setDuplicateInjectionInHierarchyFailOnError(false);
    assertAbout(javaSource())
        .that(source)
        .processedWith(processor)
        .compilesWithoutError();
  }

  @Test
  public void compilationShouldFailIfDuplicateToothpickTypeLazyInjectionFound() {
    final JavaFileObject lazySource =
        JavaFileObjects.forSourceString(
            "toothpick.Lazy",
            Joiner.on('\n')
                .join(
                    " package toothpick;",
                    "import javax.inject.Provider;",
                    "public interface Lazy<T> extends Provider<T> {}"
                ));
    final JavaFileObject source =
        JavaFileObjects.forSourceString(
              "com.groupon.android.dichecks.dummy.A",
              Joiner.on('\n')
                  .join(
                      "package com.groupon.android.dichecks.dummy;",
                      "import javax.inject.Inject;",
                      "import javax.inject.Named;",
                      "import toothpick.Lazy;",
                      "public class A {",
                      "    @Inject @Named(\"someNamedString\") String something;",
                      "    @Inject Lazy<InjectedClass> aClass;",
                      "}",
                      "class B extends A {",
                      "    @Inject InjectedClass aClass;",
                      "}",
                      "class InjectedClass {}"));

    assertAbout(javaSources())
          .that(ImmutableList.of(source, lazySource))
          .processedWith(new DiChecksProcessor())
          .failsToCompile()
          .withErrorContaining("Duplicate injection found: injected class "
                                   + "com.groupon.android.dichecks.dummy.InjectedClass in "
                                   + "com.groupon.android.dichecks.dummy.B also found in"
                                   + " com.groupon.android.dichecks.dummy.A.");
  }

  @Test
  public void compilationShouldFailIfDuplicateDaggerTypeLazyInjectionFound() {
    final JavaFileObject lazySource =
        JavaFileObjects.forSourceString(
            "dagger.Lazy",
            Joiner.on('\n')
            .join(
                " package dagger;",
                "public interface Lazy<T> { T get(); }"
            ));

    final JavaFileObject source =
        JavaFileObjects.forSourceString(
            "com.groupon.android.dichecks.dummy.A",
            Joiner.on('\n')
                .join(
                    "package com.groupon.android.dichecks.dummy;",
                    "import javax.inject.Inject;",
                    "import javax.inject.Named;",
                    "import dagger.Lazy;",
                    "public class A {",
                    "    @Inject @Named(\"someNamedString\") String something;",
                    "    @Inject Lazy<InjectedClass> aClass;",
                    "}",
                    "class B extends A {",
                    "    @Inject InjectedClass aClass;",
                    "}",
                    "class InjectedClass {}"));

    assertAbout(javaSources())
        .that(ImmutableList.of(source, lazySource))
        .processedWith(new DiChecksProcessor())
        .failsToCompile()
        .withErrorContaining("Duplicate injection found: injected class "
                                 + "com.groupon.android.dichecks.dummy.InjectedClass in "
                                 + "com.groupon.android.dichecks.dummy.B also found in "
                                 + "com.groupon.android.dichecks.dummy.A.");
  }

  @Test
  public void compilationShouldFailIfDuplicateProviderInjectionFound() {
    final JavaFileObject source =
        JavaFileObjects.forSourceString(
            "com.groupon.android.dichecks.dummy.A",
            Joiner.on('\n')
                .join(
                    "package com.groupon.android.dichecks.dummy;",
                    "import javax.inject.Inject;",
                    "import javax.inject.Named;",
                    "import javax.inject.Provider;",
                    "import com.groupon.android.dichecks.dummy.StringProvider;",
                    "public class A {",
                    "    @Inject @Named(\"someNamedString\") String something;",
                    "    @Inject StringProvider<String> stringProvider;",
                    "}",
                    "class B extends A {",
                    "    @Inject StringProvider<String> stringProvider;",
                    "}",
                    "class StringProvider<String> implements Provider {}"));

    assertAbout(javaSource())
        .that(source)
        .processedWith(new DiChecksProcessor())
        .failsToCompile()
        .withErrorContaining("Duplicate injection found: injected class "
                                 + "com.groupon.android.dichecks.dummy.StringProvider<java.lang.String> in "
                                 + "com.groupon.android.dichecks.dummy.B also found in "
                                 + "com.groupon.android.dichecks.dummy.A.");
  }
}

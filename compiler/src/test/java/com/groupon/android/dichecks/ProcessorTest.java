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

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import com.google.common.base.Joiner;
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
                    "class D extends B {}",
                    "class C extends A {}",
                    "class InjectedClass {}"));

    assert_()
        .about(javaSource())
        .that(source)
        .processedWith(new DiChecksProcessor())
        .compilesWithoutError();
  }

  @Test
  public void compilationShoulFailIfDuplicateFound() {
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

    assert_()
        .about(javaSource())
        .that(source)
        .processedWith(new DiChecksProcessor())
        .failsToCompile();
  }

  @Test
  public void compilationShoulNotFailIfDuplicateFoundAndWarningFlagSet() {
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
    assert_().about(javaSource()).that(source).processedWith(processor).compilesWithoutError();
  }
}

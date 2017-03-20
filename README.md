## Dependency Injection usage Checks

### What does it do?

DI checks is an annotation processor used to detect common issues when using dependency injection frameworks that use JSR 330 like [Toothpick](https://github.com/stephanenicolas/toothpick) or [Dagger](https://github.com/google/dagger).
When an issue is found compilation will fail.

Currently, the library has a single check that verifies a common programming error when using dependency injection: duplicating the same injection of the same dependency within a given hierarchy of types (i.e. a super class and a subclass perform the same injection, which is useless). This check is called DuplicateInjectionInHierarchy.

![Diagram](assets/DuplicateInjectionInHierarchyCheck.png)

### Why should you use it?

 * It's fast (couple of ms in production code)
 * Easy to setup (see setup section)
 * Customizable (each check can be customized to issue warnings or compiler error)
 * Works with multiple DI libraries
 * Tested on projects that rely heavily on dependency injection
 
### Setup

Just add the following line in your module's gradle file to get started:
```groovy
dependencies {
    ...
    apt "com.groupon.android.dichecks:compiler:x.y.z" <--- Add this to your dependencies.
    ...
}
```

You have fine grained control over checks with this pattern:

```groovy
apt {
    arguments {
        ...
        'com.groupon.android.dichecks.duplicateCheck.failOnError' 'true' <--- Issue warnings instead of compiler errors.
        'com.groupon.android.dichecks.duplicateCheck.enabled' 'true'     <--- Enable or disable check completely.
        ...
    }
}
```



### Future plans

We will add other useful checks related to dependency injection.

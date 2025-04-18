# Kotlin Komponent (Komp) Development Guidelines

This document provides specific information for developing with the Kotlin Komponent (Komp) library, a component-based UI library for Kotlin/JS.

## Build/Configuration Instructions

### Prerequisites
- Kotlin 2.1.10 or higher
- Gradle 7.0 or higher

### Building the Project

The project uses Kotlin Multiplatform with a focus on JavaScript (and potentially WebAssembly in the future).

```bash
# Build the project
./gradlew build

# Build only JS target
./gradlew jsJar

# Publish to Maven Local for local development
./gradlew publishToMavenLocal
```

### Configuration

The project uses the following Gradle plugins:
- Kotlin Multiplatform
- Maven Publish
- Dokka for documentation

Key configuration files:
- `build.gradle.kts` - Main build configuration
- `gradle.properties` - Contains publishing credentials and signing configuration
- `settings.gradle.kts` - Project settings and repository configuration

## Testing Information

### Running Tests

Tests are written using the Kotlin Test library and run with Karma using Chrome Headless.

```bash
# Run all tests
./gradlew jsTest

# Run browser tests
./gradlew jsBrowserTest
```

### Test Structure

Tests are located in the `src/jsTest` directory. The project uses the standard Kotlin Test library with annotations:

```kotlin
@Test
fun testSomething() {
    // Test code here
}
```

### Writing New Tests

When writing tests for Komponents:

1. Create a test class in the `src/jsTest/kotlin/nl/astraeus/komp` directory
2. Use the `@Test` annotation for test methods
3. For UI component tests:
   - Create a test DOM element using `document.createElement("div")`
   - Create your Komponent instance
   - Render it using `Komponent.create(element, komponent)`
   - Modify state and call `requestImmediateUpdate()` to test updates
   - Verify the DOM structure using assertions

### Example Test

Here's a simple test example:

```kotlin
@Test
fun testSimpleComponent() {
    // Create a test component
    val component = SimpleKomponent()
    val div = document.createElement("div") as HTMLDivElement
    
    // Render it
    Komponent.create(div, component)
    
    // Verify initial state
    assertEquals("Hello", div.querySelector("div")?.textContent)
    
    // Update state and re-render
    component.hello = false
    component.requestImmediateUpdate()
    
    // Verify updated state
    assertEquals("Good bye", div.querySelector("span")?.textContent)
}
```

## Additional Development Information

### Project Structure

- `src/commonMain` - Common Kotlin code
- `src/jsMain` - JavaScript-specific implementation
- `src/wasmJsMain` - WebAssembly JavaScript implementation (experimental)
- `src/jsTest` - JavaScript tests

### Key Components

1. **Komponent** - Base class for all UI components
   - Override `HtmlBuilder.render()` to define the component's UI
   - Use `requestUpdate()` for scheduled updates
   - Use `requestImmediateUpdate()` for immediate updates
   - Override `generateMemoizeHash()` to optimize re-renders

2. **HtmlBuilder** - Handles DOM creation and updates
   - Uses a virtual DOM-like approach to update only what changed
   - Supports including child components with `include()`
   - Handles attribute and event binding

3. **ElementExtensions** - Utility functions for DOM manipulation

### Optimization Features

- **Memoization**: Components can implement `generateMemoizeHash()` to avoid unnecessary re-renders
- **Batched Updates**: Multiple `requestUpdate()` calls are batched for performance
- **Efficient DOM Updates**: Only changed elements are updated in the DOM

### Error Handling

The library provides detailed error information when rendering fails:
- Set custom error handlers with `Komponent.setErrorHandler()`
- Enable debug logging with `Komponent.logRenderEvent = true`
- Use `debug {}` blocks in render functions for additional validation

### Code Style

- Follow Kotlin's official code style (`kotlin.code.style=official`)
- Use functional programming patterns where appropriate
- Prefer immutable state when possible
- Use descriptive names for components and methods
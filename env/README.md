# Environment Configuration System

This project uses a modular environment configuration system with support for:
- **Build variants** (debug, staging, release)
- **Platform-specific overrides** (Android, iOS, JS, JVM)
- **Type-safe Kotlin configuration** using expect/actual pattern

## File Structure

```
env/
├── .env.example              # Template and documentation
├── .env.debug                # Debug variant (default for local dev)
├── .env.debug.android        # Android-specific debug overrides
├── .env.staging              # Staging environment
├── .env.release              # Production environment
└── .env.{variant}.{platform} # Additional platform overrides
```

## Naming Convention

**Format:** `.env.{variant}[.{platform}]`

### Variants
- `debug` - Local development (default)
- `staging` - Testing/staging environment
- `release` - Production environment

### Platforms (optional)
- `android` - Android-specific values
- `ios` - iOS-specific values
- `js` - Web/JavaScript-specific values
- `jvm` - JVM/Server-specific values

## How It Works

1. **Base configuration**: Each variant has a base file (e.g., `.env.debug`)
2. **Platform overrides**: Platform-specific files override base values (e.g., `.env.debug.android`)
3. **Code generation**: Gradle task generates Kotlin expect/actual objects
4. **Compile-time resolution**: Each platform automatically uses its specific config

### Example

**env/.env.debug:**
```env
BASE_URL=http://localhost:8080
```

**env/.env.debug.android:**
```env
BASE_URL=http://10.0.2.2:8080  # Android emulator localhost
```

When building for Android, it uses `http://10.0.2.2:8080`.  
When building for iOS, it uses `http://localhost:8080` (from base config).

## Usage

### In Code

```kotlin
import eric.bitria.hexon.config.EnvConfig

// Access configuration values
val apiUrl = EnvConfig.BASE_URL
```

### Switching Variants

**Default (debug):**
```bash
./gradlew generateEnvConfig
```

**Staging:**
```bash
./gradlew generateEnvConfig -PbuildVariant=staging
```

**Production:**
```bash
./gradlew generateEnvConfig -PbuildVariant=release
```

### Build Commands

The `generateEnvConfig` task runs automatically before compilation, but you can manually regenerate:

```bash
# Generate debug config
./gradlew generateEnvConfig

# Generate and compile
./gradlew :shared:build

# Switch to release and build
./gradlew generateEnvConfig -PbuildVariant=release
./gradlew :composeApp:assembleRelease
```

## Adding New Variables

1. Add the variable to your base variant file:
   ```env
   # env/.env.debug
   BASE_URL=http://localhost:8080
   API_KEY=your_dev_api_key
   FEATURE_X_ENABLED=true
   ```

2. (Optional) Add platform-specific overrides:
   ```env
   # env/.env.debug.android
   BASE_URL=http://10.0.2.2:8080
   ```

3. Regenerate config:
   ```bash
   ./gradlew generateEnvConfig --rerun-tasks
   ```

4. Access in code:
   ```kotlin
   EnvConfig.API_KEY
   EnvConfig.FEATURE_X_ENABLED
   ```

## Creating New Variants

1. Create a new base file:
   ```bash
   cp env/.env.debug env/.env.custom
   ```

2. Edit the values:
   ```env
   # env/.env.custom
   BASE_URL=http://custom-server.com:8080
   ```

3. Generate with your variant:
   ```bash
   ./gradlew generateEnvConfig -PbuildVariant=custom
   ```

## Platform-Specific Overrides

Create platform-specific overrides when needed:

```bash
# Android needs special localhost address
env/.env.debug.android

# iOS might need different endpoint
env/.env.release.ios

# Web might use different API
env/.env.staging.js

# JVM server has its own config
env/.env.debug.jvm
```

## Generated Files

The system generates expect/actual implementations:

```
shared/src/
├── commonMain/kotlin/eric/bitria/hexon/config/
│   └── EnvConfig.kt          # expect object (declaration)
├── androidMain/kotlin/eric/bitria/hexon/config/
│   └── EnvConfig.kt          # actual object (Android values)
├── iosMain/kotlin/eric/bitria/hexon/config/
│   └── EnvConfig.kt          # actual object (iOS values)
├── jsMain/kotlin/eric/bitria/hexon/config/
│   └── EnvConfig.kt          # actual object (JS values)
└── jvmMain/kotlin/eric/bitria/hexon/config/
    └── EnvConfig.kt          # actual object (JVM values)
```

**⚠️ Do not edit generated files manually** - they are regenerated on each build.

## Git Configuration

Environment files are gitignored by default (except `.env.example`):

```gitignore
env/.env.*
!env/.env.example
```

**Share:** Only commit `.env.example` as a template.  
**Local:** Each developer maintains their own `.env.*` files.

## Troubleshooting

### Config not updating?
```bash
./gradlew generateEnvConfig --rerun-tasks
```

### Wrong variant being used?
Check that you're specifying the variant:
```bash
./gradlew clean generateEnvConfig -PbuildVariant=release
```

### Missing platform-specific config?
Platform overrides are optional. If not present, the base variant config is used.

### Compilation errors?
Ensure all variants have the same variable names. The `expect` declaration requires consistency across all platforms.

## Implementation Details

- **Task:** `GenerateEnvConfigTask` (in `buildSrc/`)
- **Trigger:** Automatically runs before Kotlin compilation
- **Cache:** Uses Gradle configuration cache for performance
- **Validation:** Warns if base variant file is missing


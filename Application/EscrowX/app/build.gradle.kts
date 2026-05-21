android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.escrowx" // Keep your package name
        minSdk = 24
        targetSdk = 36
        // ...
    }

    // MAKE SURE THIS IS ADDED INSIDE THE ANDROID BLOCK:
    buildFeatures {
        viewBinding = true
    }
}
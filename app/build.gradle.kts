plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.omnilens.omniguard'
    compileSdk 34

    defaultConfig {
        applicationId "com.omnilens.omniguard"
        minSdk 21
        targetSdk 34
        versionCode 19
        versionName "1.9.1"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    // 🎯 الكود المسؤول عن إصلاح وتحديد اسم ملف الـ APK الناتج تلقائياً
    applicationVariants.all { variant ->
        variant.outputs.all {
            outputFileName = "OmniLens-v${variant.versionName}.apk"
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.activity:activity-ktx:1.8.2'
}

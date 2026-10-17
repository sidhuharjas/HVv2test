android {
    namespace = "org.hivevisionftc"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        // The suites run on the JVM; Android framework calls (Log, etc.) return defaults instead of throwing.
        unitTests.isReturnDefaultValues = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation("org.firstinspires.ftc:RobotCore:12.0.0")
    implementation("org.firstinspires.ftc:Hardware:12.0.0")
    implementation("androidx.annotation:annotation:1.6.0")

    // Public API returns Marrow's PathResult and takes its Zone, so consumers need Marrow on
    // their compile classpath. Non-transitive: only the weaver/zones packages are used, which
    // keeps Marrow's JSON layer (Jackson) off the robot.
    api("com.skeletonarmyftc.marrow:core:1.2.1") { isTransitive = false }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

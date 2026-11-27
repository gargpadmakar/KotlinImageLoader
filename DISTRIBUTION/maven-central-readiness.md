# Maven Central Readiness Checklist & Setup Guide

This document outlines the steps and configuration required to publish **KotlinImageLoader** to Maven Central via the Central Portal (Sonatype).

---

## 1. Prerequisites Checklist

- [x] **Group ID Reservation**: Namespace `io.github.gargpadmakar` or `com.github.gargpadmakar`.
- [x] **Verified Git Repository**: Hosted on `https://github.com/gargpadmakar/KotlinImageLoader`.
- [x] **Open Source License**: Apache License, Version 2.0 included in root repository and all module POMs.
- [x] **Release Packaging**: Generates `-release.aar`, `-sources.jar`, and valid `.pom` files for all 7 modules.
- [ ] **GPG Key Pair**: Key generated and published to keyserver (keys.openpgp.org, keyserver.ubuntu.com).
- [ ] **Sonatype Central Portal Token**: Account created at [central.sonatype.com](https://central.sonatype.com).

---

## 2. GPG Key Generation

To sign releases for Maven Central:

1. **Generate a GPG key pair**:
   ```bash
   gpg --full-generate-key
   ```
   Select:
   - Kind: `(1) RSA and RSA`
   - Keysize: `4096`
   - Validity: `0` (does not expire) or `2y`
   - Real name: `Padmakar Garg`
   - Email: `techboostpg@gmail.com`

2. **List key ID**:
   ```bash
   gpg --list-secret-keys --keyid-format=long
   # Example output: rsa4096/3AA5C34371567BD2
   ```

3. **Publish public key to keyservers**:
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>
   gpg --keyserver keys.openpgp.org --send-keys <YOUR_KEY_ID>
   ```

4. **Export Secret Armored Key** (for Gradle / CI):
   ```bash
   gpg --armor --export-secret-keys <YOUR_KEY_ID> > secring.gpg
   ```

---

## 3. Gradle Signing Configuration

Add signing credentials to your local user Gradle properties (`~/.gradle/gradle.properties`):

```properties
# ~/.gradle/gradle.properties (NEVER commit this file!)
signing.keyId=3AA5C343
signing.password=YourGpgPassphrase
signing.secretKeyRingFile=C:/Users/Dell/.gnupg/secring.gpg

# Sonatype Central Portal User Token
mavenCentralUsername=YOUR_CENTRAL_PORTAL_TOKEN_USERNAME
mavenCentralPassword=YOUR_CENTRAL_PORTAL_TOKEN_PASSWORD
```

---

## 4. Gradle Build Configuration (Root `build.gradle.kts`)

Configure the signing and Maven publishing plugin in root or convention plugins:

```kotlin
subprojects {
    apply(plugin = "signing")

    configure<SigningExtension> {
        val signingKeyId = project.findProperty("signing.keyId") as String?
        val signingPassword = project.findProperty("signing.password") as String?
        val signingSecretKey = project.findProperty("signing.secretKeyRingFile") as String?

        if (signingKeyId != null && signingPassword != null && signingSecretKey != null) {
            sign(extensions.getByType<PublishingExtension>().publications)
        }
    }
}
```

---

## 5. Staging & Publication Validation

1. **Build and publish signed artifacts to staging repository**:
   ```powershell
   .\gradlew publishAllPublicationsToSonatypeRepository --max-workers 1
   ```
2. **Inspect staging repository in Sonatype Portal**:
   - Verify all 7 artifacts are present:
     - `image-core-1.1.0.aar`, `.asc`, `.md5`, `.sha1`
     - `image-domain-1.1.0.aar`, `.asc`, `.md5`, `.sha1`
     - `image-data-1.1.0.aar`, `.asc`, `.md5`, `.sha1`
     - `image-loader-1.1.0.aar`, `.asc`, `.md5`, `.sha1`
     - `image-compose-1.1.0.aar`, `.asc`, `.md5`, `.sha1`
     - `image-view-1.1.0.aar`, `.asc`, `.md5`, `.sha1`
     - `image-mvi-1.1.0.aar`, `.asc`, `.md5`, `.sha1`
     - All matching `-sources.jar` and `-sources.jar.asc`
3. **Close and Release**:
   - Sonatype validates POM metadata, signatures, and javadocs/sources.
   - Once validated, click **Release** (or automatic drop/close via plugin).
   - Artifacts sync to Maven Central search within 30 minutes.

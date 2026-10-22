Pod::Spec.new do |spec|
    spec.name                     = 'ComposeDemoLib'
    spec.version                  = '1.0.0'
    spec.homepage                 = 'https://example.com'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'ComposeDemo shared framework'
    spec.vendored_frameworks      = 'build/cocoapods/framework/ComposeDemoFramework.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '15.6'
    spec.dependency 'NMSSHT7', '2.9.1'
                
    if !Dir.exist?('build/cocoapods/framework/ComposeDemoFramework.framework') || Dir.empty?('build/cocoapods/framework/ComposeDemoFramework.framework')
        raise "

        Kotlin framework 'ComposeDemoFramework' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :composedemo:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':composedemo',
        'PRODUCT_MODULE_NAME' => 'ComposeDemoFramework',
    }
                
    spec.script_phases = [
        {
            :name => 'Build ComposeDemoLib',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
    spec.resources = ['build/compose/cocoapods/compose-resources']
end
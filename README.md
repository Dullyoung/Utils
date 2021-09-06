# Utils
some dev util
include base http lib for java
# how to use?
- Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
-  Add the dependency
```gradle
dependencies {
	        implementation 'com.github.dullyoung:utils:1.0'
	}
```

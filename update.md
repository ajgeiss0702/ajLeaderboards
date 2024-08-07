# Update checklist

This is for me when releasing updates, because otherwise I forget things

1. Update version in build.gradle.kts & commit
2. Create tag for update (e.g. 2.8.0)
3. Push tag & ver bump commit (ensure to select checkbox to push tag)
4. Merge dev -> master
5. Create release (draft release notes in release)
    * Use github compare for reference: https://github.com/ajgeiss0702/ajLeaderboards/compare/2.7.0...2.8.0
6. Double-check release notes
7. Release ajLeaderboards on
    * Modrinth
    * Hangar
    * Polymart
    * Spigot
8. Announce on discord
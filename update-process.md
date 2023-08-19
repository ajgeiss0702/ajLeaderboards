# Update process

This file is mostly just a reminder to myself of the steps I need to take when releasing an update for the plugin.

If you are not me, you do NOT follow these instructions.

1. Ensure on dev branch
2. Final bug and polish checking
3. Change version in build.gradle.kts
4. Commit with `[nolist]` then the version name. e.g. `[nolist] 2.8.0` (don't push yet)
5. Create tag with the version name on the commit just committed
6. Push
7. Merge into master
## Publish Process

**Prerequisite**: make changes to projects and test thoroughly before continuing/starting publish process.

Create a branch to track the changes to POM files as part of the publish.

* `git checkout -b {publish branch name}`

### Parent Project
* `mvn release:prepare -N -Darguments=-N`
* Accept default value for version since it's just the current version without the `-SNAPSHOT`.
* Verify new snapshot version is acceptable and change if necessary. Accept.
* `mvn release:perform`
* Obtain the token name and value to authenticate with the Sonatype API. Possible places:
  * `~/.m2/conf/settings.xml`
  * `$MAVEN_HOME/conf/settings.xml`
  * A new token can be generated at https://central.sonatype.com/usertoken.
* Calculate **Bearer Token** to use with Sonatype API:
  * `echo {token name}:{token value} | base64`
  * Copy the value somewhere temporarily.
* ```
  curl -X POST \
  -H "Authorization: Bearer {bearer token from previous step}" \
  -H "Content-Type: application/json" \
  "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/com.therealvan?publishing_type=automatic"
  ```
* Sign into https://central.sonatype.com/publishing and monitor the publish process.

### appender-core
* cd `appender-core`
* Update `pom.xml`:
  * `project.version` to `{new version}-SNAPSHOT`
  * `project.parent.version` to `{new version}` (no more `-SNAPSHOT` because the new version of the parent has just published).
* Commit changes.
* `mvn release:prepare`
* `mvn release:perform`

### appender-lof4j2
* cd `appender-core`
* Update `pom.xml`:
  * `project.version` to `{new version}-SNAPSHOT`
  * `project.parent.version` to `{new version}` (no more `-SNAPSHOT` because the new version of the parent has just published).
* Commit changes.
* `mvn release:prepare`
* `mvn release:perform`


## Functional Test

* Use https://github.com/bluedenim/log4j-s3-search-samples to use the new version to test.
* Push up changes when completed.
  * `git push origin {publish branch name}`
* Create new GitHub Release version.


## Rollback
If a rollback of `mvn:prepare` is needed:
* Run `mvn:rollback`. This will undo changes and add a `git revert` commit of previous changes.
* To allow retrying, use `git log` to see the tag created. Then delete the tag by: `git tag -d {tag name}`.
* Optional: use `git reset --hard xxxxxxx` where `xxxxxxx` is the SHA before the mvn changes.


## See Also:
* https://www.pn.therealvan.com/2025/12/22/sonatype-updates/
* https://www.pn.therealvan.com/2018/11/25/publishing-a-java-project-into-mvn-repository/

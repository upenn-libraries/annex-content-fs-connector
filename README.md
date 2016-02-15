# [git-annex](http://git-annex.branchable.com/)-aware FileSystemConnector plugin for Fedora 4

Leveraging some minor adjustments in configurability of modeshape and Fedora 4, this
project develops a plugin for Fedora 4 that enables nuanced handling (e.g., via redirect) 
of "broken" git-annex symlinks. 

## Build and test deployment

### Modeshape 4.4.0.Final-config-fs-connector

Based on the 4.4.0.Final version of modeshape currently included in Fedora 4, a fork of 
modeshape has been adjusted [with a branch](https://github.com/upenn-libraries/modeshape/tree/modeshape-4.4.0.Final-config-fs-connector)
to allow hooks in the FileSystemConnector for more nuanced handling of filesystem metadata.

Checkout this branch and build (because based on a release tag, with only minor refactoring 
changes, "skipTests" is reasonable:
```
mvn -DskipTests=true -DpreferIpv4 -s settings.xml clean install
```
The built jars may be referenced as modshape version "4.4.0.Final-symlink"

### Fedora 4 configurable-fs-connector

A fork was created [with a branch](https://github.com/upenn-libraries/fcrepo4/tree/configurable-fs-connector) based on master branch commit 99a30ffdb, adjusting modeshape dependencies to refer
to the above-mentioned "4.4.0.Final-symlink" version, and providing protected (subclass) access 
to some checksum-cache-related convenience methods in the FedoraFileSystemConnector class.

Checkout this branch and build (again, skipTests optional, potentially reasonable given the 
minor extent of changes): 
```
mvn -DskipTests=true clean install
```

## Use and development

Adjust the `JETTY_CONSOLE_JAR` environment variable in update-plugin.sh to point to the 
"jetty-console" jar from your built version of the Fedora 4 `configurable-fs-connector` 
branch, and adjust the `repository-symlink.json` `externalSources.directoryPath` to point 
to a directory containing broken symlinks (e.g., a git-annex repository, or whatever you 
desire).



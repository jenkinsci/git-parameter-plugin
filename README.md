# Git Parameter
<a href='https://ci.jenkins.io/job/Plugins/job/git-parameter-plugin/'><img src='https://ci.jenkins.io/buildStatus/icon?job=Plugins/git-parameter-plugin/master'></a>

This plugin allows you to assign git branch, tag, pull request or
revision number as parameter in your builds.

**Important!**   
There is no need to set up anything special in plugin settings.  
*This plugin will read GIT SCM configuration from your projects.*  
This plugin used directly the [Git
Plugin](https://plugins.jenkins.io/git/) and [Git
Client Plugin](https://plugins.jenkins.io/git-client/).

## Basic configuration

#### Project configuration
![project configuration image](docs/images/image2018-9-20_22-0-7.png)

#### [Build with Parameters](http://xps15:8083/job/git_parameter/build?delay=0sec) form
![Build with Parameters image](docs/images/image2018-9-20_22-2-47.png)

## Example pipeline script

**Important!**   
Works with version 0.9.4 or greater

#### Branch type - Basic usage

* Declarative Pipeline
```groovy
// Using git without checkout
pipeline {
  agent any
  parameters {
    gitParameter branchFilter: 'origin/(.*)', defaultValue: 'master', name: 'BRANCH', type: 'PT_BRANCH'
  }
  stages {
    stage('Example') {
      steps {
        git branch: "${params.BRANCH}", url: 'https://github.com/jenkinsci/git-parameter-plugin.git'
      }
    }
  }
}
```

* Scripted Pipeline
```groovy
properties([
    parameters([
        gitParameter(branch: '',
                     branchFilter: 'origin/(.*)',
                     defaultValue: 'master',
                     description: '',
                     name: 'BRANCH',
                     quickFilterEnabled: false,
                     selectedValue: 'NONE',
                     sortMode: 'NONE',
                     tagFilter: '*',
                     type: 'PT_BRANCH')
    ])
])
node {
    git branch: "${params.BRANCH}", url: 'https://github.com/jenkinsci/git-parameter-plugin.git'
}
```

###### Important settings:

-   It should be set a `default` value because initial build must get
    this information
-   Using `git` should be set a `branchFilter` as `*origin/(.\*)*`
    (origin is a remote server name)

###### Parameter type

-   `PT_TAG`
-   `PT_BRANCH`
-   `PT_BRANCH_TAG`
-   `PT_REVISION`
-   `PT_PULL_REQUEST`

**Important!**  
If you need to use other type (other then branch) parameter, you must use git within `checkout` 

#### Tag type
```groovy
// Using git within checkout
pipeline {
    agent any
    parameters {
        gitParameter name: 'TAG',
                     type: 'PT_TAG',
                     defaultValue: 'master'
    }
    stages {
        stage('Example') {
            steps {
                checkout([$class: 'GitSCM',
                          branches: [[name: "${params.TAG}"]],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'https://github.com/jenkinsci/git-parameter-plugin.git']]
                        ])
            }
        }
    }
}
```

#### Branch Tag type
```groovy
pipeline {
    agent any
    parameters {
        gitParameter name: 'BRANCH_TAG',
                     type: 'PT_BRANCH_TAG',
                     defaultValue: 'master'
    }
    stages {
        stage('Example') {
            steps {
                checkout([$class: 'GitSCM',
                          branches: [[name: "${params.BRANCH_TAG}"]],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'https://github.com/jenkinsci/git-parameter-plugin.git']]
                        ])
            }
        }
    }
}
```

#### Revision type
```groovy
pipeline {
    agent any
    parameters {
        gitParameter name: 'REVISION',
                     type: 'PT_REVISION',
                     defaultValue: 'master'
    }
    stages {
        stage('Example') {
            steps {
                checkout([$class: 'GitSCM',
                          branches: [[name: "${params.REVISION}"]],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'https://github.com/jenkinsci/git-parameter-plugin.git']]
                        ])
            }
        }
    }
}
```

#### Pull Request type
```groovy
pipeline {
    agent any
    parameters {
        gitParameter name: 'PULL_REQUESTS',
                     type: 'PT_PULL_REQUEST',
                     defaultValue: '1',
                     sortMode: 'DESCENDING_SMART'
    }
    stages {
        stage('Example') {
            steps {
                checkout([$class: 'GitSCM',
                branches: [[name: "pr/${params.PULL_REQUESTS}/head"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [],
                gitTool: 'Default',
                submoduleCfg: [],
                userRemoteConfigs: [[refspec: '+refs/pull/*:refs/remotes/origin/pr/*', url: 'https://github.com/jenkinsci/git-parameter-plugin.git']]])
            }
        }
    }
}
```

## Options

#### Parameter Type
Name using in pipeline

```groovy
type: 'PT_TAG' or 'PT_BRANCH' or 'PT_BRANCH_TAG' or 'PT_REVISION' or 'PT_PULL_REQUEST'
```

Explains about PT\_TAG or PT\_BRANCH or PT\_BRANCH\_TAG:

Plugin using [git ls-remote](https://git-scm.com/docs/git-ls-remote.html) command to get
remote tags or branches, this solution was implemented in  [
JENKINS-40232](https://issues.jenkins-ci.org/browse/JENKINS-40232).

In code plugin
[use](https://github.com/jenkinsci/git-client-plugin/blob/9f2a3ec48e699222ce3034dfe14cdb319e563ed5/src/main/java/org/jenkinsci/plugins/gitclient/GitClient.java#L631)ing 
getRemoteReferences from GitClient, look implementation
in [CliGitAPIImpl](https://github.com/jenkinsci/git-client-plugin/blob/master/src/main/java/org/jenkinsci/plugins/gitclient/CliGitAPIImpl.java). 

```java
package org.jenkinsci.plugins.gitclient
//...

public interface GitClient {
//...
    Map<String, ObjectId> getRemoteReferences(String remoteRepoUrl, String pattern, boolean headsOnly, boolean tagsOnly) throws GitException, InterruptedException;
//...
}
```

#### Branch
Name using in pipeline

```groovy
branch
```

#### Branch Filter
Name using in pipeline

```groovy
branchFilter
```

#### Tag Filter
Name using in pipeline

```groovy
tagFilter
```

#### Sort Mode
Name using in pipeline

```groovy
sortMode: 'NONE' or 'ASCENDING_SMART' or 'DESCENDING_SMART' or 'ASCENDING' or 'DESCENDING'
```

You can select the following sorting options for
tags/revision/branches/branches\_or\_tags/pull requests

-   none
-   descending
-   ascending
-   ascending smart
-   descending smart

For the smart variants the compare treats a sequence of digits as a
single character. Contributed by Graeme Hill.

#### Default Value
Name using in pipeline

```groovy
defaultValue
```

In release 0.9.9 or later it is good to set a default value, because this
value is using the initial build (in Pipeline).  
Default value is returned when some error occurred on getting data.

![default value](docs/images/image2019-2-16_22-46-54.png)

#### Selected Value
Name using in pipeline

```groovy
selectedValue: 'NONE' or 'TOP' or 'DEFAULT'
```

#### Use repository
Name using in pipeline

```groovy
useRepository
```

**Remember!**  
You don't set a git repository into the plugin, this plugin
using git repositories which are defined in project in SCM section!

If in the task are defined multiple repositories, this option specifies
which the repository is taken into account on getting data.  
*If the option is not defined, is taken a first defined repository.*  
This option is a regular expression, which is compared to the
'Repository URL'.

You can define the multiple SCM for few way, you can use [Multiple SCMs
Plugin](https://plugins.jenkins.io/multiple-scms/), specified
many 'Repository URL' in one SCM  or define them in pipeline.

Consider an example based on two repositories:

-   <https://github.com/klimas7/exampleA.git>
-   <https://github.com/klimas7/exampleB.git>

**Pipeline: Complex example**

```groovy
pipeline {
    agent any
    parameters {
        gitParameter branchFilter: 'origin.*/(.*)', defaultValue: 'master', name: 'BRANCH_A', type: 'PT_BRANCH', useRepository: '.*exampleA.git'
        gitParameter branchFilter: 'origin.*/(.*)', defaultValue: 'master', name: 'BRANCH_B', type: 'PT_BRANCH', useRepository: '.*exampleB.git'

    }
    stages {
        stage('Example') {
            steps {
                git branch: "${params.BRANCH_A}", url: 'https://github.com/klimas7/exampleA.git'
                git branch: "${params.BRANCH_B}", url: 'https://github.com/klimas7/exampleB.git'
            }
        }
    }
}
```

After initial run you get 

!['build with parameters' section](docs/images/image2018-9-21_22-47-52.png)

Example when 'Use repository' is not set:

**Pipeline: Use repository is not set**

```groovy
pipeline {
    agent any
    parameters {
        gitParameter branchFilter: 'origin.*/(.*)', defaultValue: 'master', name: 'BRANCH', type: 'PT_BRANCH'
    }
    stages {
        stage('Example') {
            steps {
                git url: 'https://github.com/klimas7/exampleA.git'
                dir('dir-for-exampleB') {
                    git url: 'https://github.com/klimas7/exampleB.git'
		}
            }
        }
    }
}
```

After initial run you get 

!['build with parameters' section](docs/images/image2018-9-21_23-3-22.png)

#### Quick Filter

```groovy
quickFilterEnabled
```

#### List Size

```groovy
listSize
```

## Global configuration

**Important!**   
Works with version 0.9.9 or greater

![show 'need to clone' information](docs/images/image2019-2-16_22-26-39.png)

![parameter values](docs/images/image2019-2-17_13-5-14.png)

## Error handling

**Important!**   
Works with version 0.9.9 or greater

If an error occurred while retrieving data, the default value is
returned.  
Additional information is provided below, along with the cause of the
error.

Examples:
1. This error occur when the repository is not configured or 'Use
repository' option not match with any repository.
![error handling 1](docs/images/image2019-2-17_17-2-14.png)
1. This error occur when the repository is not exists or URL is wrong.
![error handling 2](docs/images/image2019-2-17_12-49-47.png)
1. This error occur when there are no ssh command on Jenkins master.
![error handling 3](docs/images/image2019-2-17_17-4-32.png)

## Contribute
* Visit https://jenkins.io/doc/developer/publishing/ for "information about developing Jenkins-Plugins"

* You may checkout/clone this project and build it by simply calling `mvn clean install` in the root of the checkout. Test your changes by going to your Jenkins-CI site and import the generated `target/git-parameter.hpi` by going to your base URL + `jenkins/pluginManager/advanced`. There you find an option to upload a plugin.

* The Jenkins-CI of this plugin can be seen at [DEV@cloud](https://ci.jenkins.io/job/Plugins/job/git-parameter-plugin/).  

#### Pull Request Policy

If you want to add some changes for this plugin:  
Add the issue in jenkins [JIRA](https://issues.jenkins-ci.org) to the component git-parameter-plugin  
Describe there why you need change the plugin.

#### TODO

* Add a new method `listRemoteTags(URL)` to git-client-plugin to use. Will speed up listing tags and avoids cloning/fetching the content.

* Fix the pending issues from
	* Even though the GIT SCM module has the ability to provide "credentials" (SSH key) for the repository, the git-parameter plugin doesn't seem to use them. "Issue lukanus":https://github.com/lukanus/git-parameter/issues/14
* Allow translations by converting all html stuff to jelly
* Add explanation when configuring the sort mode
* Allow regular expressions when sorting.
* Better testing. How to we test the configuration dialog? How do we test whether correct tags are listed when a user triggers a build?

## Development history

This plugin was offered to the community by lukanus (Łukasz Miłkowski <lukanus@uaznia.net>) end of 2011. He was active till February 2012.

In May 2014 ngiger (Niklaus Giger niklaus.giger) decided to maintain this plugin and bring in the various improvements made by others.

March 2016 klimas7 (Boguslaw Klimas) he began to the care and maintenance of the plugin.. We will see ... :)

## Changelog
For recent versions, see [GitHub Releases](https://github.com/jenkinsci/git-parameter-plugin/releases)

For versions 0.9.11 and older, see the [legacy CHANGELOG](https://github.com/jenkinsci/git-parameter-plugin/blob/git-parameter-0.9.19/CHANGELOG.md)

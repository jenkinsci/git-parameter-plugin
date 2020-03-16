## Changelog

### 17/06/2019 Version 0.9.11

-   [JENKINS-57879](https://issues.jenkins-ci.org/browse/JENKINS-57879) Build number increments by more than 1
-   [JENKINS-51512](https://issues.jenkins-ci.org/browse/JENKINS-51512) Git parameter plugin does not work with Windows agent
-   [JENKINS-50711](https://issues.jenkins-ci.org/browse/JENKINS-50711) plugin doesn't work with ssh link to git repo
-   [JENKINS-52051](https://issues.jenkins-ci.org/browse/JENKINS-52051) git-parameter exec: nc: not found

### 18/02/2019 Version 0.9.10
* PR #78: changed select element's id following previous fix in ae9de22


### 16/02/2019 Version 0.9.9

-   [JENKINS-55769](https://issues.jenkins-ci.org/browse/JENKINS-55769): Tag
    match filter shows more entries than direct command (git tag -l
    "$tagFilter")
-   [JENKINS-55770](https://issues.jenkins-ci.org/browse/JENKINS-55770): Intrusive
    and misleading warning text from the parameter selection display
-   [JENKINS-54359](https://issues.jenkins-ci.org/browse/JENKINS-54359): Change
    error handling

### 09/10/2018 Version 0.9.6

-   [JENKINS-53911](https://issues.jenkins-ci.org/browse/JENKINS-53911): Duplicate
    entries in list, if git repo is checked out twice.
-   [JENKINS-52533](https://issues.jenkins-ci.org/browse/JENKINS-52533): Display
    commit message on the build action
-   [JENKINS-45925](https://issues.jenkins-ci.org/browse/JENKINS-45925): Show
    git commit message when building with parameter "revision"

### 17/09/2018 Version 0.9.5

-   [JENKINS-51041](https://issues.jenkins-ci.org/browse/JENKINS-51041), [JENKINS-50510](https://issues.jenkins-ci.org/browse/JENKINS-50510), [JENKINS-45419](https://issues.jenkins-ci.org/browse/JENKINS-45419): Fixed
    use repository option

### 16/08/2018 Version 0.9.4

-   [JENKINS-52273](https://issues.jenkins-ci.org/browse/JENKINS-52273): Support
    git-parameter-plugin in declarative pipeline
-   [JENKINS-52132](https://issues.jenkins-ci.org/browse/JENKINS-52132):
    Description with HTML support  

### 20/06/2018 Version 0.9.3

-   [JENKINS-51521](https://issues.jenkins-ci.org/browse/JENKINS-51521): Git
    parameter does not show branch list in case deleteDir() is present
    in pipeline
-   [JENKINS-51476](https://issues.jenkins-ci.org/browse/JENKINS-51476):
    Git parameter plugin is not retrieving revision number  

### 16/04/2018 Version 0.9.2

-   [JENKINS-50776](https://issues.jenkins-ci.org/browse/JENKINS-50776):
    Default selected item doesn’t always honor exactly Default Value
-   [JENKINS-49727](https://issues.jenkins-ci.org/browse/JENKINS-49727):
    Add optional parameter to specify the number of items the list will
    display 

### 18/02/2018 Version 0.9.1

-   [JENKINS-45419](https://issues.jenkins-ci.org/browse/JENKINS-45419): 'Use
    Repository' setting does not find other remote urls if multiple
    repos are added to job
-   [PR
    \#55](https://github.com/jenkinsci/git-parameter-plugin/pull/55): Add
    complete French support

### 02/11/2017 Version 0.9.0

-   [JENKINS-47110](https://issues.jenkins-ci.org/browse/JENKINS-47110 "View this issue"): Retrieving
    Git references do not work with variable in Repository URL
-   [PR
    \#54](https://github.com/jenkinsci/git-parameter-plugin/pull/54): Help
    improvement: complete English translation, and reorder items
-   [JENKINS-47078](https://issues.jenkins-ci.org/browse/JENKINS-47078 "View this issue"): IndexOutOfBoundsException
    for pipeline job
-   [JENKINS-39530](https://issues.jenkins-ci.org/browse/JENKINS-39530): Add
    support to Pipeline projects

### 04/09/2017 Version 0.8.1

-   [JENKINS-46216](https://issues.jenkins-ci.org/browse/JENKINS-46216 "View this issue"): Null
    Pointer exception when no default parameter provided
-   [JENKINS-45577](https://issues.jenkins-ci.org/browse/JENKINS-45577 "View this issue"): \[Git
    Parameter Plugin\] Parameter does not support the definition from
    CLI
-   [JENKINS-46624](https://issues.jenkins-ci.org/browse/JENKINS-46624 "View this issue"): fix
    remote name
-   [JENKINS-46185](https://issues.jenkins-ci.org/browse/JENKINS-46185): Set
    browser focus to filter after the QuickFilter has been filled
-   [JENKINS-46038](https://issues.jenkins-ci.org/browse/JENKINS-46038): Extend
    list of supported type with pull request
-   [JENKINS-26799](https://issues.jenkins-ci.org/browse/JENKINS-26799):
    Multiple SCMs plugin support part 3 (Work fine when selected
    revisions)
-   [JENKINS-42313](https://issues.jenkins-ci.org/browse/JENKINS-42313): Default
    Value not honoured

### 02/06/2017 Version 0.8.0

-   [JENKINS-26799](https://issues.jenkins-ci.org/browse/JENKINS-26799):
    Multiple SCMs plugin support part 2
-   [JENKINS-40523](https://issues.jenkins-ci.org/browse/JENKINS-40523):
    Include Jenkins Project Name in Log message
-   [JENKINS-40232](https://issues.jenkins-ci.org/browse/JENKINS-40232):
    Git Parameter Plugin doesn't need to clone

### 23/01/2017 Version 0.7.2

-   [JENKINS-41091](https://issues.jenkins-ci.org/browse/JENKINS-41091):
    git-parameter:0.7.1 breaks the multi-line parameters in rebuild

### 11/27/2016 Version 0.7.1

-   [JENKINS-39366](https://issues.jenkins-ci.org/browse/JENKINS-39366):
    Add support for a rebuild-plugin
-   [JENKINS-26799](https://issues.jenkins-ci.org/browse/JENKINS-26799):
    Multiple SCMs plugin support

### 09/12/2016 Version 0.7.0

-   [JENKINS-37555](https://issues.jenkins-ci.org/browse/JENKINS-37555):
    Better support for internationalization
-   [JENKINS-37595](https://issues.jenkins-ci.org/browse/JENKINS-37595):
    Add support for polish localization
-   [JENKINS-37370](https://issues.jenkins-ci.org/browse/JENKINS-37370):
    Retrieving Git references do not work with variable in "Repository
    URL"
-   [JENKINS-37953](https://issues.jenkins-ci.org/browse/JENKINS-37953):
    Add support to ProxySCM
-   [JENKINS-37738](https://issues.jenkins-ci.org/browse/JENKINS-37738):
    Update dependency in plugin and cleanup in pom
-   [JENKINS-34876](https://issues.jenkins-ci.org/browse/JENKINS-34876):
    Git Parameters not working for Pipeline projects and Jenkinsfile
    from SCM

### 08/06/2016 Version 0.6.2

-   [JENKINS-36833](https://issues.jenkins-ci.org/browse/JENKINS-36833):
    Race Condition Populating Multiple Tag Parameters
-   [JENKINS-36934](https://issues.jenkins-ci.org/browse/JENKINS-36934):
    No return value passed to the url
-   [JENKINS-31939](https://issues.jenkins-ci.org/browse/JENKINS-31939):
    The top value is better to be chosen by default of to have such
    option (part 3)

### 07/19/2016 Version 0.6.1

-   [JENKINS-31939](https://issues.jenkins-ci.org/browse/JENKINS-31939):
    The top value is better to be chosen by default of to have such
    option (part 2)

### 07/06/2016 Version 0.6.0

-   [JENKINS-36104](https://issues.jenkins-ci.org/browse/JENKINS-36104):
    Add Repo SCM support (Derron Hu)
-   [JENKINS-16290](https://issues.jenkins-ci.org/browse/JENKINS-16290):
    git parameter plugin doesn't support Jenkins slave setup with git
    repos checked out only on slaves
-   [JENKINS-35363](https://issues.jenkins-ci.org/browse/JENKINS-35363):
    Git parameter filter doesn't work

### 05/03/2016 Version 0.5.1

-   [JENKINS-34425](https://issues.jenkins-ci.org/browse/JENKINS-34425):
    Git parameter plugin skips one build number while populating tags
-   [JENKINS-34544](https://issues.jenkins-ci.org/browse/JENKINS-34544):
    NPE After update to 0.5.0
-   [JENKINS-34574](https://issues.jenkins-ci.org/browse/JENKINS-34574):
    After cleaned workspace doesn't show branches

### 04/02/2016 Version 0.5.0

1\. User interface

-   [JENKINS-27435](https://issues.jenkins-ci.org/browse/JENKINS-27435):
    Quick branch filter (Thank Bruno P. Kinoshita for inspiration)
-   [JENKINS-33963](https://issues.jenkins-ci.org/browse/JENKINS-33963):
    Branch filter does not save the value (part of the work Joe Hansche)

2\. Refactor/fix/feature

-   [JENKINS-33361](https://issues.jenkins-ci.org/browse/JENKINS-33361):
    Long release number in branch, tag or revision name
-   [JENKINS-33084](https://issues.jenkins-ci.org/browse/JENKINS-33084):
    Git Parameter plugin should prune stale remote
    branches (@darashenka)
-   [JENKINS-31939](https://issues.jenkins-ci.org/browse/JENKINS-31939):
    The top value is better to be chosed by default of to have such
    option
-   [JENKINS-33831](https://issues.jenkins-ci.org/browse/JENKINS-33831):
    Revision Parameter Type: ArrayIndexOutOfBoundsException
-   [JENKINS-33912](https://issues.jenkins-ci.org/browse/JENKINS-33912):
    Refactoring Test Case

### 01/16/2015 Version 0.4.0

-   Possibility to select branch, tag or branch (Alban Dericbourg)
-   Keep complex logics in jelly as less as possible (Yestin Sun)
-   support folders (Nicolas De Loof)
-   Minimized pom.xml as suggested by Jesse Glick
-   Removed LICENSE.txt as suggested by Jesse Glick

### 05/14/14 Version 0.3.2

User visible changes are:

-   Updated help texts for configuration and when selecting your
    tag/revision
-   Runs a fetch each time the user enters the "Build with parameter".
-   Run clone when fetch fails on workspace empty (Gabor Liptak)
-   Merging SortMode from graeme-hill
-   With an empty workspace the tags are calculated after we made a
    checkout. This may take a long time.

Changes relevant to developer

-   Added MIT-LICENSE.txt to use the same license as Jenkins-CI.
-   Added Contributors.textile
-   Display month not minutes in date. Add HH.mm. Display only first 8
    chars of SHA1 (Niklaus Giger)
-   Add backup pluginRepository (Gabor Liptak)
-   Use GitTool to query configured location of the git executable
    (gliptak)
-   Upgrade to git 2.2.0. (christ66)
-   Build against latest stable Jenkins-CI version 1.554.1
-   New co-maintainer/developer Niklaus Giger (id ngiger)
-   Version 0.3 and 0.3.1 never made it to the distribution, because of
    problems with the release mechanism.

### 02/21/12 Version 0.2

-   Corrected error - plugin wasn't showing anything after change of
    main Git Plugin
-   Corrected major dis-functionality - plugin now it showing revisions
    only from correct job/project.
-   Adding support for choosing branch from which revisions/tags are
    returned

### 11/01/11 Version 0.1

-   Initial Release

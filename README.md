# Git branch or tag selection

The Jenkins git-parameter plugin is broken in multiple ways. If it works for
you, that's fine. It didn't work for me.

If you want to select a git tag or branch, you are better off to use the
dynamic parameter plugin and write some groovy to call a shell script that
collects tags or branches.

# Select a tag

Make your build parameterized and add a "dynamic choice parameter". Put this
in the "choices script" field:

    def command = """/opt/get_tags.sh jobname"""
    def proc = command.execute()
    proc.waitFor()
    def list = proc.in.text.tokenize()

The /opt/get_tags.sh script looks like this:

    #!/bin/bash
    cd /var/lib/jenkins/workspace/$1
    git fetch --tags >/dev/null
    git tag | sort -r

Jobname is the name of the Jenkins job. In this case it has to be as string
with no spaces.


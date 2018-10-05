package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.plugins.git.GitException;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RevisionInfoFactoryTest {
    private static final String COMMIT_HASH_1 = "ee650d9b5dbc39ec1bdfc6608f49db94ce8d7be4";
    private static final ObjectId SHA1_1 = ObjectId.fromString(COMMIT_HASH_1);
    private static final String[] RAW_1 = {"tree ee650d9b5dbc39ec1bdfc6608f49db94ce8d7be4",
            "parent 3409e0e7a3c0a2887b14c95d803b90f9314606aa",
            "author klimas7 <klimas7@gmail.com> 1523905899 +0200",
            "committer klimas7 <klimas7@gmail.com> 1523905899 +0200",
            "",
            "    Version 0.9.2",
            "",
            ":100644 100644 ab9cfc8ef1c067ef36fb45741be8b9444ba7085c a01738c8f727254fdcf9d03fcb0965567104a31e M\tREADME.textile"};


    private static final String COMMIT_HASH_2 = "b9a246e842a5478fe01b52eb93e0e23cdb79f616";
    private static final ObjectId SHA1_2 = ObjectId.fromString(COMMIT_HASH_2);
    private static final String[] RAW_2 = {"tree b9a246e842a5478fe01b52eb93e0e23cdb79f616",
            "parent 3409e0e7a3c0a2887b14c95d803b90f9314606aa",
            "author klimas7 <klimas7@gmail.com> 2018-05-23T15:28:13+0200",
            "committer klimas7 <klimas7@gmail.com> 2018-05-23T15:28:13+0200"};

    private static final String COMMIT_HASH_3 = "b9a246e842a5478fe01b52eb93e0e23cdb79f616";
    private static final ObjectId SHA1_3 = ObjectId.fromString(COMMIT_HASH_3);
    private static final String[] RAW_NO_AUTHOR = {"tree b9a246e842a5478fe01b52eb93e0e23cdb79f616",
            "parent 3409e0e7a3c0a2887b14c95d803b90f9314606aa",
            "committer klimas7 <klimas7@gmail.com> 2018-05-23T15:28:13+0200"};
    private static final String SHORT_COMMIT_HASH_3 = COMMIT_HASH_3.substring(0, 8);

    private static final String COMMIT_HASH_4 = "f36014bb502c66259cace1ac6a42317cb120d926";
    private static final ObjectId SHA1_4 = ObjectId.fromString(COMMIT_HASH_4);
    private static final String[] RAW_4 = {"commit d13e0b12d645e68d15f52cdf32ec98c9731fe3b8",
            "tree f36014bb502c66259cace1ac6a42317cb120d926",
            "parent 54a7ea2118c56af484075016e3867512b65a75b6",
            "author Nick Whelan <nickw@indeed.com> 1423782469 -0600",
            "committer Joe Hansche <jhansche@meetme.com> 1427401175 -0400",
            "",
            "    Performance improvements",
            "    ",
            "    Performance improvements when listing tags and branches. It's not necessary",
            "    to perform a fetch operation when listing remote tags and branches, if",
            "    using ls-remote.",
            "",
            ":100644 100644 12504dff00708931ca5bd94faa8d4d91fd964e26 6faa30e2cff5d93c1425d85308acb0b7cad21539 M\tsrc/main/java/net/uaznia/lukanus/hudson/plugins/gitparameter/GitParameterDefinition.java",
            ":100644 100644 792aab5dfe8f6925d56012e97a87802dab926f60 be1db20696d5ffea041c86e6cea13828b95e19bf M\tsrc/main/resources/net/uaznia/lukanus/hudson/plugins/gitparameter/GitParameterDefinition/config.jelly",
            ":100644 000000 54b37e567a56583bc61fef860bcd45b946e2053c 0000000000000000000000000000000000000000 D\tsrc/main/resources/net/uaznia/lukanus/hudson/plugins/gitparameter/GitParameterDefinition/help-branchfilter.html",
            ":000000 100644 0000000000000000000000000000000000000000 71b8e86fa081074b4df8f2ba4fa3321b763be722 A\tsrc/main/resources/net/uaznia/lukanus/hudson/plugins/gitparameter/GitParameterDefinition/help-filter.html",
            ":100644 100644 c5bc2243cd449f88c31262208b142a98620f9f90 4580a726212cd65f5438a79350ad1fa002bbdd11 M\tsrc/test/java/net/uaznia/lukanus/hudson/plugins/gitparameter/GitParameterDefinitionTest.java"};


    @Test
    public void testGetRevisions() throws InterruptedException {
        GitClient gitClient = mock(GitClient.class);
        when(gitClient.revListAll()).thenReturn(Arrays.asList(SHA1_1, SHA1_2, SHA1_4));
        when(gitClient.showRevision(SHA1_1)).thenReturn(Arrays.asList(RAW_1));
        when(gitClient.showRevision(SHA1_2)).thenReturn(Arrays.asList(RAW_2));
        when(gitClient.showRevision(SHA1_4)).thenReturn(Arrays.asList(RAW_4));

        RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(gitClient, null);
        List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

        assertEquals(3, revisions.size());
        assertEquals("ee650d9b 2018-04-16 21:11 klimas7 <klimas7@gmail.com> Version 0.9.2", revisions.get(0).getRevisionInfo());
        assertEquals("b9a246e8 2018-05-23T15:28:13+0200 klimas7 <klimas7@gmail.com>", revisions.get(1).getRevisionInfo());
        assertEquals("f36014bb 2015-02-12 17:07 Nick Whelan <nickw@indeed.com> Performance improvements Performance improvements when listing tags and branches. It's not necessary to perform a fetch operation when listing remote ...", revisions.get(2).getRevisionInfo());
    }

    @Test
    public void testNoAuthor() throws InterruptedException {
        GitClient gitClient = mock(GitClient.class);
        when(gitClient.revListAll()).thenReturn(Arrays.asList(SHA1_3));
        when(gitClient.showRevision(SHA1_3)).thenReturn(Arrays.asList(RAW_NO_AUTHOR));

        RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(gitClient, null);
        List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

        assertEquals(1, revisions.size());
        RevisionInfo revisionInfo = revisions.get(0);
        assertEquals(SHORT_COMMIT_HASH_3, revisionInfo.getRevisionInfo());
        assertEquals(COMMIT_HASH_3, revisionInfo.getSha1());
    }

    @Test
    public void testGitException() throws InterruptedException {
        GitClient gitClient = mock(GitClient.class);
        when(gitClient.revListAll()).thenReturn(Arrays.asList(SHA1_3));
        when(gitClient.showRevision(SHA1_3)).thenThrow(new GitException());

        RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(gitClient, null);
        List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

        assertEquals(1, revisions.size());
        RevisionInfo revisionInfo = revisions.get(0);
        assertEquals(SHORT_COMMIT_HASH_3, revisionInfo.getRevisionInfo());
        assertEquals(COMMIT_HASH_3, revisionInfo.getSha1());
    }
}
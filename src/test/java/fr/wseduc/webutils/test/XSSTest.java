package fr.wseduc.webutils.test;

import org.junit.Test;

import static org.owasp.html.AntiSamyTest.*;

public class XSSTest {

    @Test
    public void antiSamyTests() throws Exception {
        testScriptAttacks();
        testImgAttacks();
        testHrefAttacks();
        testCssAttacks();
        testIllegalXML();
    }


}

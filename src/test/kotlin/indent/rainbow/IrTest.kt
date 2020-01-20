package indent.rainbow

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class IrTest : BasePlatformTestCase() {

    fun testBasic() {
        // TODO
        //  We should use LightJavaCodeInsightFixtureTestCase,
        //  Because Java-related functionality is not available in BasePlatformTestCase
        //  But I can't find how to add dependency on com.intellij.modules.java only for tests
        if (Language.findLanguageByID("JAVA") == null) return


        val content = """
            public class Foo {
            
                void foo(int x,
                         int y) {}  // line 3
            
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText(StdFileTypes.JAVA, content)
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!

        val startOffset = document.getLineStartOffset(3)

        val indentHelper = IrIndentHelper.getInstance(psiFile)!!
        val (indent, alignment) = indentHelper.getIndentAndAlignment(startOffset)!!

        val useTabs = indentHelper.indentOptions.USE_TAB_CHARACTER
        if (useTabs) {
            assert(indent == 1)
            assert(alignment == 9)
        } else {
            assert(indent == 4)
            assert(alignment == 9)
        }
    }
}

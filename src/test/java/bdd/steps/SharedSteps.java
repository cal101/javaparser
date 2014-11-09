package bdd.steps;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.stmt.Statement;
import org.hamcrest.CoreMatchers;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;

public class SharedSteps {

    /* Map that maintains shares state across step classes.  If manipulating the objects in the map you must update the state */
    private Map<String, Object> state;

    public SharedSteps(Map<String, Object> state){
        this.state = state;
    }

    @Given("a CompilationUnit")
    public void givenACompilationUnit() {
        state.put("cu1", new CompilationUnit());
    }

    @Given("a second CompilationUnit")
    public void givenASecondCompilationUnit() {
        state.put("cu2", new CompilationUnit());
    }

    @When("the following source is parsed:$classSrc")
    public void whenTheFollowingSourceIsParsed(String classSrc) throws ParseException {
        state.put("cu1", JavaParser.parse(new ByteArrayInputStream(classSrc.getBytes())));
    }

    @When("the following sources is parsed by the second CompilationUnit:$classSrc")
    public void whenTheFollowingSourcesIsParsedBytTheSecondCompilationUnit(String classSrc) throws ParseException {
        state.put("cu2", JavaParser.parse(new ByteArrayInputStream(classSrc.getBytes())));
    }


    @When("the \"$fileName\" is parsed")
    public void whenTheJavaFileIsParsed(String fileName) throws IOException, ParseException {
        URL url = getClass().getResource("../samples/" + fileName);
        CompilationUnit compilationUnit = JavaParser.parse(new File(url.getPath()));
        state.put("cu1", compilationUnit);
    }

    @Then("the CompilationUnit is equal to the second CompilationUnit")
    public void thenTheCompilationUnitIsEqualToTheSecondCompilationUnit() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit) state.get("cu2");

        assertThat(compilationUnit, is(equalTo(compilationUnit2)));
    }

    @Then("the CompilationUnit has the same hashcode to the second CompilationUnit")
    public void thenTheCompilationUnitHasTheSameHashcodeToTheSecondCompilationUnit() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit) state.get("cu2");

        assertThat(compilationUnit.hashCode(), is(equalTo(compilationUnit2.hashCode())));
    }

    @Then("the CompilationUnit is not equal to the second CompilationUnit")
    public void thenTheCompilationUnitIsNotEqualToTheSecondCompilationUnit() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit) state.get("cu2");

        assertThat(compilationUnit, not(equalTo(compilationUnit2)));
    }

    @Then("the CompilationUnit has a different hashcode to the second CompilationUnit")
    public void thenTheCompilationUnitHasADifferentHashcodeToTheSecondCompilationUnit() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit) state.get("cu2");

        assertThat(compilationUnit.hashCode(), not(equalTo(compilationUnit2.hashCode())));
    }


    @Then("the expected source should be:$classSrc")
    public void thenTheExpectedSourcesShouldBe(String classSrc) {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        assertThat(compilationUnit.toString(), CoreMatchers.is(equalToIgnoringWhiteSpace(classSrc)));
    }

    public static BodyDeclaration getMemberByTypeAndPosition(TypeDeclaration typeDeclaration, int position,
                                                       Class<? extends BodyDeclaration> typeClass){
        int typeCount = 0;
        for(BodyDeclaration declaration : typeDeclaration.getMembers()){
            if(declaration.getClass().equals(typeClass)){
                if(typeCount == position){
                    return declaration;
                }
                typeCount++;
            }
        }
        throw new IllegalArgumentException("No member " + typeClass + " at position: " + position );
    }
}

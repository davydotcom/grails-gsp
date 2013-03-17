package grails.test.mixin

import grails.artefact.Artefact
import grails.converters.JSON
import grails.converters.XML
import grails.test.mixin.web.ControllerUnitTestMixin
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mime.MimeUtility
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.web.multipart.MultipartFile

/**
 * @author Graeme Rocher
 */
@TestMixin(ControllerUnitTestMixin)
class AstEnhancedControllerUnitTestMixinTests extends GroovyTestCase{

    void testRenderText() {
        def controller = getMockController()

        controller.renderText()
        assert response.contentAsString == "good"
    }

    protected getMockController() {
        mockController(AnotherController)
    }

    void testSimpleControllerRedirect() {

        def controller = getMockController()

        controller.redirectToController()

        assert response.redirectedUrl == '/bar'
    }

    void testRenderView() {
        def controller = getMockController()

        controller.renderView()

        assert "/another/foo" == controller.modelAndView.viewName
    }

    void testRenderXml() {
        def controller = getMockController()

        controller.renderXml()

        assert "<book title='Great'/>" == controller.response.contentAsString
        assert "Great" == controller.response.xml.@title.text()
    }

//    void testRenderJson() {
//
//        def controller = getMockController()
//
//        controller.renderJson()
//
//        assert '{"book":"Great"}' == controller.response.contentAsString
//        assert "Great" == controller.response.json.book
//    }

    void testRenderAsJson() {

        def controller = getMockController()

        controller.renderAsJson()

        assert '{"foo":"bar"}' == controller.response.contentAsString
        assert "bar" == controller.response.json.foo
    }

    void testRenderState() {
        params.foo = "bar"
        request.bar = "foo"
        def controller = getMockController()

        controller.renderState()

        def xml = response.xml

        assert xml.parameter.find { it.@name == 'foo' }.@value.text() == 'bar'
        assert xml.attribute.find { it.@name == 'bar' }.@value.text() == 'foo'
    }

    void testInjectedProperties() {
        assert request != null
        assert response != null
        assert servletContext != null
        assert params != null
        assert grailsApplication != null
        assert applicationContext != null
        assert webRequest != null
    }

    void testControllerAutowiring() {
        messageSource.addMessage("foo.bar", request.locale, "Hello World")

        def controller = getMockController()

        controller.renderMessage()

        assert 'Hello World' == controller.response.contentAsString
    }

    void testRenderWithFormatXml() {
        def controller = getMockController()

        response.format = 'xml'
        controller.renderWithFormat()

        assert '<?xml version="1.0" encoding="UTF-8"?><map><entry key="foo">bar</entry></map>' == response.contentAsString
    }

    void testRenderWithFormatHtml() {
        def controller = getMockController()

        response.format = 'html'
        def model = controller.renderWithFormat()

        assert model?.foo == 'bar'
    }

    void testWithFormTokenSynchronization() {

        def controller = getMockController()
        controller.renderWithForm()

        assert "Bad" == response.contentAsString

        def holder = SynchronizerTokensHolder.store(session)
        def token = holder.generateToken('/test')
        params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
        params[SynchronizerTokensHolder.TOKEN_KEY] = token

        response.reset()

        controller.renderWithForm()

        assert "Good" == response.contentAsString
    }

    void testFileUpload() {
        def controller = getMockController()

        final file = new GrailsMockMultipartFile("myFile", "foo".bytes)
        request.addFile(file)
        controller.uploadFile()

        assert file.targetFileLocation.path == "${File.separatorChar}local${File.separatorChar}disk${File.separatorChar}myFile"
    }

    void testRenderBasicTemplateNoTags() {
        def controller = getMockController()

        groovyPages['/another/_bar.gsp'] = 'Hello <%= 10 %>'
        controller.renderTemplate()

        assert response.contentAsString == "Hello 10"
    }

    void testRenderBasicTemplateWithTags() {
        def controller = getMockController()
        messageSource.addMessage("foo.bar", request.locale, "World")

        groovyPages['/another/_bar.gsp'] = 'Hello <g:message code="foo.bar" />'
        controller.renderTemplate()

        assert response.contentAsString == "Hello World"
    }

    void testRenderBasicTemplateWithLinkTag() {
        def controller = getMockController()

        groovyPages['/another/_bar.gsp'] = 'Hello <g:createLink controller="bar" />'
        controller.renderTemplate()

        assert response.contentAsString == "Hello /bar"
    }

    void testInvokeTagLibraryMethod() {

        def controller = getMockController()
        controller.renderTemplateContents()

        assert response.contentAsString == "/foo"
    }

    void testInvokeTagLibraryMethodViaNamespace() {

        def controller = getMockController()

        groovyPages['/another/_bar.gsp'] = 'Hello <g:message code="foo.bar" />'

        controller.renderTemplateContentsViaNamespace()

        assert response.contentAsString == "Hello foo.bar"
    }
}

@Artefact("Controller")
class AnotherController {

    def handleCommand = { TestCommand test ->

         if (test.hasErrors()) {
             render "Bad"
         }
         else {
             render "Good"
         }
    }
    def uploadFile = {
        assert request.method == 'POST'
        assert request.contentType == "multipart/form-data"
        MultipartFile file = request.getFile("myFile")
        file.transferTo(new File("/local/disk/myFile"))
    }

    def renderTemplateContents = {
        def contents = createLink(controller:"foo")
        render contents
    }
    def renderTemplateContentsViaNamespace = {
        def contents = g.render(template:"bar")

        render contents
    }
    def renderText = {
        render "good"
    }

    def redirectToController = {
        redirect(controller:"bar")
    }

    def renderView = {
        render(view:"foo")
    }

    def renderTemplate = {
        render(template:"bar")
    }

    def renderXml = {
        render(contentType:"text/xml") {
            book(title:"Great")
        }
    }

    def renderJson = {
        render(contentType:"text/json") {
            book = "Great"
        }
    }

    def renderAsJson = {
        render([foo:"bar"] as JSON)
    }

    def renderWithFormat = {
        def data = [foo:"bar"]
        withFormat {
            xml { render data as XML }
            html data
        }
    }

    def renderState = {
        render(contentType:"text/xml") {
            println params.foo
            println request.bar
            requestInfo {
                for (p in params) {
                    parameter(name:p.key, value:p.value)
                }
                request.each {
                    attribute(name:it.key, value:it.value)
                }
            }
        }
    }

    MessageSource messageSource
    @Autowired
    MimeUtility mimeUtility

    @Autowired
    LinkGenerator linkGenerator

    def renderMessage() {
        assert mimeUtility !=null
        assert linkGenerator != null
        render messageSource.getMessage("foo.bar", null, request.locale)
    }

    def renderWithForm() {
        withForm {
            render "Good"
        }.invalidToken {
            render "Bad"
        }
    }
}

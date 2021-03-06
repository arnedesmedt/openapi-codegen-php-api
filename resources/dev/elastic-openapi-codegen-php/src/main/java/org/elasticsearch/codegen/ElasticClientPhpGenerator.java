package org.elasticsearch.codegen;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.PhpClientCodegen;
import org.openapitools.codegen.utils.ModelUtils;

public class ElasticClientPhpGenerator extends PhpClientCodegen implements CodegenConfig {

  public static final String GENERATOR_NAME         = "elastic-php-client";
  public static final String HELP_URL               = "helpUrl";
  public static final String COPYRIGHT              = "copyright";
  public static final String CLIENT_CLASS           = "clientClass";
  public static final String CLIENT_CLASS_QUALIFIER = "clientClassQualifier";

  public ElasticClientPhpGenerator() {
    super();

    cliOptions.add(new CliOption(HELP_URL, "Help URL"));
    cliOptions.add(new CliOption(COPYRIGHT, "Copyright"));
    cliOptions.add(new CliOption(CLIENT_CLASS, "Client file"));

    this.setTemplateDir(ElasticClientPhpGenerator.GENERATOR_NAME);
    this.setSrcBasePath("");
    this.embeddedTemplateDir = this.templateDir();

    this.apiDirName = "Endpoint";
    setApiPackage(getInvokerPackage() + "\\" + apiDirName);
    this.setParameterNamingConvention("camelCase");
  }

  @Override
  public void processOpts() {
    super.processOpts();

    String clientClass = (String) additionalProperties.getOrDefault(CLIENT_CLASS, "Client");
    additionalProperties.put(CLIENT_CLASS, clientClass);

    if (clientClass.startsWith("Abstract")) {
      additionalProperties.put(CLIENT_CLASS_QUALIFIER, "abstract");
    }

    this.resetTemplateFiles();

    supportingFiles.add(new SupportingFile("Client.mustache", "", clientClass.concat(".php")));
    supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
  }

  @Override
  public CodegenType getTag() {
    return CodegenType.CLIENT;
  }

  @Override
  public String getName() {
    return ElasticClientPhpGenerator.GENERATOR_NAME;
  }

  @Override
  public String toApiName(String name) {
    return initialCaps(name);
  }

  @Override
  @SuppressWarnings("static-method")
  public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
      String uniqueName = co.operationId;
      co.operationIdLowerCase = uniqueName.toLowerCase(Locale.ROOT);
      co.operationIdCamelCase = org.openapitools.codegen.utils.StringUtils.camelize(uniqueName);
      co.operationIdSnakeCase = org.openapitools.codegen.utils.StringUtils.underscore(uniqueName);

      if (co.vendorExtensions == null) {
        co.vendorExtensions = new HashMap<>();
      }

      co.vendorExtensions.put("x-operation-scope", co.vendorExtensions.getOrDefault("x-operation-scope", "public"));
      co.vendorExtensions.put("x-add-to-documentation", co.vendorExtensions.getOrDefault("x-operation-scope", "public").equals("public"));

      operations.put(uniqueName, Arrays.asList(co));
  }

  @Override
  @SuppressWarnings("rawtypes")
  public String getTypeDeclaration(Schema p) {
    if (ModelUtils.isArraySchema(p) || ModelUtils.isMapSchema(p)) {
      return "array";
    } else if (ModelUtils.isObjectSchema(p) || ModelUtils.isModel(p) || StringUtils.isNotBlank(p.get$ref())) {
      return "array";
    }

    return super.getTypeDeclaration(p);
  }

  @Override
  public String getTypeDeclaration(String name) {
    if (!languageSpecificPrimitives.contains(name)) {
      return "array";
    }

    return super.getTypeDeclaration(name);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public CodegenOperation fromOperation(String path,
                                        String httpMethod,
                                        Operation operation,
                                        Map<String, Schema> schemas,
                                        OpenAPI openAPI) {
      processOperation(operation, openAPI);
      CodegenOperation op = super.fromOperation(path, httpMethod, operation, schemas, openAPI);

      return op;
  }

  private void processOperation(Operation operation, OpenAPI openAPI) {
      RequestBody requestBody = ModelUtils.getReferencedRequestBody(openAPI, operation.getRequestBody());
      if (requestBody != null) {
          processRequestBody(requestBody, openAPI);
      }
  }

  private void processRequestBody(RequestBody requestBody, OpenAPI openAPI) {
      requestBody.getContent();
      Schema<?> schema = ModelUtils.getReferencedSchema(openAPI, ModelUtils.getSchemaFromRequestBody(requestBody));
  }

  @Override
  public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {
    CodegenParameter codegenParameter = super.fromParameter(parameter, imports);

    if (parameter.getExtensions() != null && parameter.getExtensions().containsKey("x-codegen-param-name")) {
        codegenParameter.paramName = parameter.getExtensions().get("x-codegen-param-name").toString();
    }

    return codegenParameter;
  }

  private void resetTemplateFiles() {
    this.supportingFiles.clear();
    this.apiTemplateFiles.clear();
    this.apiTestTemplateFiles.clear();
    this.apiDocTemplateFiles.clear();
    this.modelTemplateFiles.clear();
    this.modelTestTemplateFiles.clear();
    this.modelDocTemplateFiles.clear();

    apiTemplateFiles.put("api.mustache", ".php");
  }
}

package io.shreyash.rush.migrator;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesActivities;
import com.google.appinventor.components.annotations.UsesActivityMetadata;
import com.google.appinventor.components.annotations.UsesApplicationMetadata;
import com.google.appinventor.components.annotations.UsesAssets;
import com.google.appinventor.components.annotations.UsesBroadcastReceivers;
import com.google.appinventor.components.annotations.UsesContentProviders;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.UsesServices;
import com.google.appinventor.components.annotations.androidmanifest.ActivityElement;
import com.google.appinventor.components.annotations.androidmanifest.MetaDataElement;
import com.google.appinventor.components.annotations.androidmanifest.ProviderElement;
import com.google.appinventor.components.annotations.androidmanifest.ReceiverElement;
import com.google.appinventor.components.annotations.androidmanifest.ServiceElement;
import com.google.auto.service.AutoService;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.shreyash.rush.migrator.util.XmlUtil;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
    "com.google.appinventor.components.annotations.DesignerComponent",
    "com.google.appinventor.components.annotations.SimpleObject",
    "com.google.appinventor.components.annotations.UsesActivities",
    "com.google.appinventor.components.annotations.UsesActivityMetadata",
    "com.google.appinventor.components.annotations.UsesApplicationMetadata",
    "com.google.appinventor.components.annotations.UsesAssets",
    "com.google.appinventor.components.annotations.UsesBroadcastReceivers",
    "com.google.appinventor.components.annotations.UsesContentProviders",
    "com.google.appinventor.components.annotations.UsesLibraries",
    "com.google.appinventor.components.annotations.UsesNativeLibraries",
    "com.google.appinventor.components.annotations.UsesPermissions",
    "com.google.appinventor.components.annotations.UsesServices",
    "com.google.appinventor.components.annotations.androidmanifest.ActionElement",
    "com.google.appinventor.components.annotations.androidmanifest.ActivityElement",
    "com.google.appinventor.components.annotations.androidmanifest.CategoryElement",
    "com.google.appinventor.components.annotations.androidmanifest.DataElement",
    "com.google.appinventor.components.annotations.androidmanifest.GrantUriPermissionElement",
    "com.google.appinventor.components.annotations.androidmanifest.IntentFilterElement",
    "com.google.appinventor.components.annotations.androidmanifest.MetaDataElement",
    "com.google.appinventor.components.annotations.androidmanifest.PathPermissionElement",
    "com.google.appinventor.components.annotations.androidmanifest.ProviderElement",
    "com.google.appinventor.components.annotations.androidmanifest.ReceiverElement",
    "com.google.appinventor.components.annotations.androidmanifest.ServiceElement"
})
public class Migrator extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
    final String outputDir = processingEnv.getOptions().get("outputDir");

    final Messager messager = processingEnv.getMessager();

    for (Element el : roundEnv.getElementsAnnotatedWith(DesignerComponent.class)) {
      SimpleObject so = el.getAnnotation(SimpleObject.class);
      if (so.external()) {
        messager.printMessage(Diagnostic.Kind.NOTE, "External component class named \"" +
            el.getSimpleName().toString() + "\" detected.");
        try {
          generateAndroidManifest(el, outputDir);
          generateRushYml(el, outputDir);
        } catch (TransformerException | ParserConfigurationException | IOException e) {
          e.printStackTrace();
        }
      }
    }

    return false;
  }

  /**
   * Generates rush.yml for {@param comp}.
   *
   * @param comp      the element for which rush.yml is to be produced
   * @param outputDir the path where the generated rush.yml is to be stored
   */
  private void generateRushYml(Element comp, String outputDir) throws IOException {
    final String moreInfo = "# For a detailed info on this file and supported fields, check out this" +
        "\n# link: https://github.com/ShreyashSaitwal/rush-cli/wiki/Metadata-File\n";

    final StringBuilder content = new StringBuilder();

    final String extName = comp.getSimpleName().toString();
    final DesignerComponent dc = comp.getAnnotation(DesignerComponent.class);

    // Add extension name
    content.append(moreInfo + "\n---\n")
        .append("name: " + extName + "\n");

    // Add description
    content.append("description: ")
        .append(!dc.description().equals("") ? dc.description()
            : "Extension component for " + extName + ". Built with Rush.")
        .append("\n\n");

    // Add version info
    content.append("version: \n")
        .append("  number: " + dc.version() + "\n")
        .append("  name: '")
        .append(!dc.versionName().equals("") ? dc.versionName() : dc.version())
        .append("'\n\n");

    // Add build info
    content.append("build:")
        .append("  release:")
        .append("    optimize: true");

    // Add minimum SDK
    content.append("min_sdk: " + dc.androidMinSdk() + "\n\n");

    // Add assets (icon and others)
    content.append("assets: \n")
        .append("  icon: ")
        .append(!dc.iconName().equals("") ? dc.iconName() : "icon.png")
        .append("\n");
    final UsesAssets ua = comp.getAnnotation(UsesAssets.class);
    if (ua != null) {
      content.append("  other:\n");
      for (final String file : ua.fileNames().split(",")) {
        content.append("    - " + file.trim() + "\n");
      }
    }
    content.append("\n");

    // Add deps if any
    final UsesLibraries ul = comp.getAnnotation(UsesLibraries.class);
    if (ul != null) {
      content.append("deps:\n");
      for (final String lib : ul.libraries().split(",")) {
        content.append("  - " + lib.trim() + "\n");
      }
    }

    // Write rush-<extname>.yml to `outputDir`
    final Path yamlPath = Paths.get(outputDir + File.separatorChar + "rush-" + extName + ".yml");
    final FileWriter writer = new FileWriter(yamlPath.toFile());
    writer.write(content.toString());
    writer.flush();
    writer.close();
  }

  /**
   * Generates AndroidManifest.xml for {@param comp}
   *
   * @param comp      the element for which AndroidManifest.xml is to be generated
   * @param outputDir the path to where the generated manifest file is to br stored.
   */
  private void generateAndroidManifest(Element comp, String outputDir)
      throws TransformerException, ParserConfigurationException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

    final Document doc = documentBuilder.newDocument();
    doc.setXmlVersion("1.0");

    final String namespaceUri = "http://schemas.android.com/apk/res/android";

    final org.w3c.dom.Element root = doc.createElement("manifest");
    doc.appendChild(root);

    final Attr versionCodeAttr = doc.createAttributeNS(namespaceUri, "android:versionCode");
    versionCodeAttr.setValue("1");
    final Attr versionNameAttr = doc.createAttributeNS(namespaceUri, "android:versionName");
    versionNameAttr.setValue("1.0");

    root.setAttributeNode(versionCodeAttr);
    root.setAttributeNode(versionNameAttr);

    final org.w3c.dom.Element applicationElement = doc.createElement("application");
    root.appendChild(applicationElement);

    // Add permission
    final UsesPermissions usesPermissions = comp.getAnnotation(UsesPermissions.class);
    if (usesPermissions != null) {
      final String[] permissions = usesPermissions.permissionNames().split(",");
      for (String permission : permissions) {
        final org.w3c.dom.Element permissionEl = doc.createElement("uses-permission");
        final Attr attr = doc.createAttributeNS(namespaceUri, "android:name");
        attr.setValue(permission);
        permissionEl.setAttributeNode(attr);
        root.appendChild(permissionEl);
      }
    }

    final XmlUtil xmlUtil = new XmlUtil();

    // Add activities
    final UsesActivities usesActivities = comp.getAnnotation(UsesActivities.class);
    if (usesActivities != null) {
      for (ActivityElement activityElement : usesActivities.activities()) {
        xmlUtil.appendElement(activityElement, doc, applicationElement, namespaceUri);
      }
    }

    // Add activity metadata
    final UsesActivityMetadata usesActivityMetadata = comp.getAnnotation(UsesActivityMetadata.class);
    if (usesActivityMetadata != null) {
      for (MetaDataElement metaDataElement : usesActivityMetadata.metaDataElements()) {
        xmlUtil.appendElement(metaDataElement, doc, applicationElement, namespaceUri);
      }
    }

    // Add application metadata
    final UsesApplicationMetadata usesApplicationMetadata = comp.getAnnotation(UsesApplicationMetadata.class);
    if (usesApplicationMetadata != null) {
      for (MetaDataElement metaDataElement : usesApplicationMetadata.metaDataElements()) {
        xmlUtil.appendElement(metaDataElement, doc, applicationElement, namespaceUri);
      }
    }

    // Add receivers
    final UsesBroadcastReceivers usesBroadcastReceivers = comp.getAnnotation(UsesBroadcastReceivers.class);
    if (usesBroadcastReceivers != null) {
      for (ReceiverElement receiverElement : usesBroadcastReceivers.receivers()) {
        xmlUtil.appendElement(receiverElement, doc, applicationElement, namespaceUri);
      }
    }

    // Add providers
    final UsesContentProviders usesContentProviders = comp.getAnnotation(UsesContentProviders.class);
    if (usesContentProviders != null) {
      for (ProviderElement providerElement : usesContentProviders.providers()) {
        xmlUtil.appendElement(providerElement, doc, applicationElement, namespaceUri);
      }
    }

    // Add services
    final UsesServices usesServices = comp.getAnnotation(UsesServices.class);
    if (usesServices != null) {
      for (ServiceElement serviceElement : usesServices.services()) {
        xmlUtil.appendElement(serviceElement, doc, applicationElement, namespaceUri);
      }
    }

    final DOMSource domSource = new DOMSource(doc);
    final Path manifestPath = Paths.get(outputDir + File.separatorChar + "manifest-"
        + comp.getSimpleName().toString() + ".xml");
    final StreamResult streamResult = new StreamResult(manifestPath.toFile());

    final TransformerFactory tf = TransformerFactory.newInstance();
    final Transformer transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    transformer.transform(domSource, streamResult);
  }
}

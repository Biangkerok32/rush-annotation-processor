package io.shreyash.rush.util;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.amihaiemil.eoyaml.exceptions.YamlReadingException;
import io.shreyash.rush.model.ExtensionFieldInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class InfoFilesGenerator {
  private final String projectRootPath;
  private final String extVersion;
  private final String org;
  private final ExtensionFieldInfo extensionFieldInfos;
  private final String outputPath;

  public InfoFilesGenerator(String projectRootPath, String extVersion, String org, ExtensionFieldInfo extensionFieldInfos, String outputPath) {
    this.projectRootPath = projectRootPath;
    this.extVersion = extVersion;
    this.org = org;
    this.extensionFieldInfos = extensionFieldInfos;
    this.outputPath = outputPath;
  }

  /**
   * Generates the simple_components.json file.
   *
   * @throws IOException
   */
  public void generateSimpleCompJson() throws IOException, JSONException {
    JSONArray simpleCompJson = new JSONArray();

    YamlMapping yml = getRushYml();
    String name = yml.string("name");
    String type = org + name;
    String verName = yml.yamlMapping("version").string("name");
    String helpStr = yml.string("description");
    String helpUrl = yml.string("helpUrl");
    String icon = yml.yamlMapping("assets").string("icon");
    int minSdk = yml.integer("min_sdk");

    JSONObject obj = new JSONObject();
    obj.put("type", type);
    obj.put("name", name);
    obj.put("versionName", verName);
    obj.put("version", extVersion);
    obj.put("helpString", helpStr);
    obj.put("helpUrl", helpUrl);
    obj.put("icon", icon);
    obj.put("androidMinSdk", minSdk);
    obj.put("external", "true");
    obj.put("dateBuilt", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE));
    obj.put("categoryString", "EXTENSION");
    obj.put("showOnPalette", "true");
    obj.put("onVisible", "true");
    obj.put("events", extensionFieldInfos.getEventJson());
    obj.put("methods", extensionFieldInfos.getFuncJson());
    obj.put("properties", extensionFieldInfos.getPropsJson());
    obj.put("blockProperties", extensionFieldInfos.getBlockPropsJson());

    simpleCompJson.put(obj);

    FileWriter writer = new FileWriter(Paths.get(outputPath, "simple_components.json").toFile());
    simpleCompJson.write(writer);
    writer.flush();
    writer.close();
  }

  /**
   * Generate simple_components_build_info.json file.
   *
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  public void generateBuildInfoJson() throws IOException, ParserConfigurationException, SAXException {
    JSONArray buildInfoJson = new JSONArray();
    JSONObject obj = new JSONObject();

    YamlMapping yml = getRushYml();
    String type = org + yml.string("name");
    int minSdk = yml.integer("min_sdk");
    obj.put(type, type);
    obj.put("androidMinSdk", new JSONArray().put(minSdk));

    // TODO: Put native libraries

    // Put libraries
    JSONArray deps = new JSONArray();
    YamlSequence ymlDeps = yml.yamlSequence("dependencies");
    if (ymlDeps != null && !ymlDeps.values().isEmpty()) {
      for (YamlNode dep : ymlDeps.values()) {
        // TODO: Once the dep management is added, remove this check
        if (dep.type().equals(com.amihaiemil.eoyaml.Node.MAPPING)) {
          deps.put(dep.asMapping().string("path"));
        } else {
          throw new YamlReadingException();
        }
      }
    }
    obj.put("libraries", deps);

    // Put assets
    JSONArray assets = new JSONArray();
    YamlSequence ymlAssets = yml.yamlMapping("assets").yamlSequence("other");
    if (ymlAssets != null && !ymlAssets.values().isEmpty()) {
      for (Object asset : ymlAssets.values().toArray()) {
        assets.put(asset.toString());
      }
    }
    obj.put("assets", assets);

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(Paths.get(projectRootPath, "AndroidManifest.xml").toFile());

    // Put application elements
    putApplicationElements(obj, doc);

    // Put permissions
    NodeList nodes = doc.getElementsByTagName("uses-permission");
    JSONArray permissions = new JSONArray();
    if (nodes.getLength() != 0) {
      for (int i = 0; i < nodes.getLength(); i++) {
        permissions.put("\"" + generateXmlElement(nodes.item(i), "manifest") + "\"");
      }
    }
    obj.put("permissions", permissions);

    buildInfoJson.put(obj);
    FileWriter writer = new FileWriter(Paths.get(outputPath, "simple_components_build_info.json").toFile());
    buildInfoJson.write(writer);
    writer.flush();
    writer.close();
  }

  /**
   * Get rush.yml file's data
   *
   * @return The rush.yml file's data
   * @throws IOException
   */
  private YamlMapping getRushYml() throws IOException {
    File rushYml = Paths.get(projectRootPath, "rush.yml").toFile();
    if (!rushYml.exists()) {
      if (Paths.get(projectRootPath, "rush.yaml").toFile().exists()) {
        rushYml = Paths.get(projectRootPath, "rush.yaml").toFile();
      } else {
        throw new FileNotFoundException("Extension processor is unable to find rush.yml metadata file.");
      }
    }

    return Yaml.createYamlInput(rushYml).readYamlMapping();
  }

  /**
   * Checks if the doc is a valid AndroidManifest.xml
   * Note: The checks aren't exhaustive.
   *
   * @param doc        The AndroidManifest.xml of this extension
   * @param extPackage The package name of the extension
   * @return true if the manifest file is valid
   */
  private boolean isXmlValid(Document doc, String extPackage) {
    Element root = doc.getDocumentElement();
    if (!root.getNodeName().equals("manifest")) {
      return false;
    } else {
      return root.getAttribute("package").equals(extPackage);
    }
  }

  /**
   * Returns a JSON array of specific XML elements from the given
   * list of nodes.
   *
   * @param node   A XML node, for eg., <service>
   * @param parent Name of the node who's child nodes we want to
   *               generate. This is required because getElementsByTag()
   *               method returns all the elements that satisfy the
   *               name.
   * @return A JSON array containing XML elements
   */
  private String generateXmlElement(Node node, String parent) {
    if (node.getNodeName().equals("uses-permission")) {
      Node permission = node.getAttributes().getNamedItem("android:name");
      if (permission != null) {
        return "<" + permission.getNodeValue() + "/>";
      } else {
        throw new DOMException((short) 1, "No permission android:name attribute found in <uses-permission>");
      }
    }

    StringBuilder sb = new StringBuilder();
    if (node.getNodeType() == Node.ELEMENT_NODE && node.getParentNode().getNodeName().equals(parent)) {
      Element element = (Element) node;
      String tagName = element.getTagName();
      sb.append("<" + tagName + " ");


      if (element.hasAttributes()) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
          if (attributes.item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
            Attr attribute = (Attr) attributes.item(i);
            sb.append(attribute.getNodeName() + " = \\\"" + attribute.getNodeValue() + "\\\" ");
          }
        }
      }

      if (element.hasChildNodes()) {
        sb.append(" >\\n");
        NodeList children = element.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
          sb.append(generateXmlElement(children.item(j), element.getNodeName()));
        }
        sb.append("</" + tagName + ">\\n");
      } else {
        sb.append("/>\\n");
      }

    }
    return sb.toString();
  }

  /**
   * Puts all the application level manifest tags buildInfoJson
   *
   * @param buildInfoJson The simple_component_build_info's JSONObject
   * @param doc           The AndroidManifest.xml document
   */
  private void putApplicationElements(JSONObject buildInfoJson, Document doc) {
    HashMap<String, String> available = new HashMap<>();
    available.put("activities", "activity");
    available.put("metadata", "meta-data");
    available.put("broadcastReceivers", "receiver");
    available.put("services", "service");
    available.put("contentProviders", "provider");

    ArrayList<String> unavailable = new ArrayList<>();
    unavailable.add("activity-alias");
    unavailable.add("uses-library");

    available.forEach((key, val) -> {
      JSONArray arr = new JSONArray();
      NodeList elements = doc.getElementsByTagName(val);
      if (elements.getLength() != 0) {
        for (int i = 0; i < elements.getLength(); i++) {
          arr.put("\"" + generateXmlElement(elements.item(i), "application") + "\"");
        }
      }
      buildInfoJson.put(key, arr);
    });

    // This is a sort of hack which allows adding application level tags
    // to AndroidManifest by adding your desired tag any of the available
    // tag's JSON array. AI2's compiler doesn't perform any checks and
    // simply add anything from this JSON arrays to the manifest...
    unavailable.forEach(el -> {
      JSONArray arr = new JSONArray();
      NodeList elements = doc.getElementsByTagName(el);
      if (elements.getLength() != 0) {
        for (int i = 0; i < elements.getLength(); i++) {
          arr.put("\"" + generateXmlElement(elements.item(i), "application") + "\"");
        }
      }
      buildInfoJson.put("metadata", arr);
    });
  }
}
# Migration Plan: Jakarta XML Bind → Jackson XML

## Overview
Replace `jakarta.xml.bind` with `jackson-dataformat-xml` across 3 packages (~61 files) using
native Jackson annotations — no bridge (`jackson-module-jaxb-annotations`). Incremental approach:
one package at a time. Jakarta deps removed last, after all three packages compile cleanly.

## Decisions
- **Replacement**: `com.fasterxml.jackson.dataformat:jackson-dataformat-xml`
- **No bridge**: clean native Jackson annotations throughout
- **MaterialDTO lifecycle**: refactor inline — `@JsonGetter`/`@JsonSetter` for UOM, density
  conversion moved into `asMaterial()`
- **Polymorphic lists**: `@JsonTypeInfo(use=NAME, include=WRAPPER_OBJECT)` + `@JsonSubTypes`
- **Out of scope**: SimpleSAX (.ork read), OpenRocketSaver (.ork write), PreferencesExporter (DOM)

---

## Phase 1 — Dependencies & Modules
*(Add Jackson now; defer Jakarta removal to Phase 6)*

### `core/build.gradle`
- Add: `implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.19.x'`
- Defer removal of: `jakarta.activation-api`, `jaxb-runtime`, `yasson`
- If Jackson JARs are not JPMS modules, add `automaticModule(...)` entry in `extraJavaModuleInfo`

### `core/src/main/java/module-info.java`
- Add: `requires com.fasterxml.jackson.dataformat.xml;`
- Add: `requires com.fasterxml.jackson.databind;`
- Defer removal of: `requires jakarta.xml.bind;` and `requires jakarta.activation;`

### `swing/src/main/java/module-info.java`
- Add: `requires com.fasterxml.jackson.core;` (for `JacksonException`)
- Defer removal of: `requires jakarta.xml.bind;`

---

## Phase 2 — Custom Type Handlers
*(Create before touching DTOs that reference them)*

### `core/src/main/java/info/openrocket/core/file/rasaero/CustomDoubleAdapter.java`
Rewrite (keep filename or rename — no callers outside rasaero/export):
- `CustomDoubleSerializer extends StdSerializer<Double>`: same `DecimalFormat("#.####", Locale.US)` logic
- `CustomDoubleDeserializer extends StdDeserializer<Double>`: same `Double.parseDouble(s)` logic
- Remove `extends XmlAdapter<String,Double>` and `import jakarta.xml.bind.annotation.adapters.XmlAdapter`

### `core/src/main/java/info/openrocket/core/file/rasaero/CustomBooleanAdapter.java`
Rewrite:
- `CustomBooleanSerializer extends StdSerializer<Boolean>`: "True"/"False" output logic
- `CustomBooleanDeserializer extends StdDeserializer<Boolean>`: `"true".equalsIgnoreCase(s)` logic
- Remove `extends XmlAdapter<String,Boolean>`

### `core/src/main/java/info/openrocket/core/preset/xml/BaseComponentDTO.java`
- Add inner class `Base64Serializer extends StdSerializer<byte[]>`: use `java.util.Base64.getEncoder().encodeToString(value)`
- Add inner class `Base64Deserializer extends StdDeserializer<byte[]>`: use `java.util.Base64.getDecoder().decode(value)`
- Remove inner class `Base64Adapter extends XmlAdapter<String,byte[]>`
- Remove `import jakarta.xml.bind.DatatypeConverter`

---

## Phase 3 — `preset/xml` Package (18 files)
*Hardest phase — has both read (unmarshal) and write (marshal). Verify after completion.*

### 3a — Enum DTOs (3 files)
Files: `ShapeDTO.java`, `MaterialTypeDTO.java`, `MaterialGroupDTO.java`
- Remove `@XmlEnum` annotation — Jackson serializes enums by name by default
- Remove `import jakarta.xml.bind.annotation.XmlEnum`

### 3b — Simple Component DTOs (11 files)
Files: `BulkheadDTO`, `CenteringRingDTO`, `EngineBlockDTO`, `InnerBodyTubeDTO`, `LaunchLugDTO`,
`NoseConeDTO`, `ParachuteDTO`, `RailButtonDTO`, `StreamerDTO`, `TransitionDTO`, `TubeCouplerDTO`

Apply annotation mapping (see table at end of document):
- `@XmlRootElement(name="X")` → `@JacksonXmlRootElement(localName="X")`
- `@XmlAccessorType(XmlAccessType.FIELD)` → remove
- `@XmlElement(name="X")` → `@JacksonXmlProperty(localName="X")`
- `@XmlElementWrapper(name="X")` → `@JacksonXmlElementWrapper(localName="X")`
- `@XmlTransient` → `@JsonIgnore`
- `@XmlType(propOrder={...})` → `@JsonPropertyOrder({...})`
- Remove all `import jakarta.*` lines; add corresponding `import com.fasterxml.jackson.*` lines

### 3c — `MaterialDTO.java`
- Add `@JsonIgnore` to the `uom` field
- Add `@JsonGetter("UnitsOfMeasure")` returning `uom.replace(Chars.SQUARED,'2').replace(Chars.CUBED,'3')`
- Add `@JsonSetter("UnitsOfMeasure")` setting `uom = v.replace('2',Chars.SQUARED).replace('3',Chars.CUBED)`
- Add `@JacksonXmlProperty(localName="UnitsOfMeasure", isAttribute=true)` on the getter
- Move density unit conversion (`uomUnit.fromUnit(density)`) from `afterUnmarshal()` into `asMaterial()`
  before the `Databases.findMaterial(...)` call
- Delete `beforeMarshal(Marshaller)` and `afterUnmarshal(Unmarshaller, Object)` methods
- Remove `import jakarta.xml.bind.Marshaller` and `import jakarta.xml.bind.Unmarshaller`

### 3d — `BaseComponentDTO.java`
- Replace `@XmlInlineBinaryData` + `@XmlJavaTypeAdapter(Base64Adapter.class)` on the image field
  with `@JsonSerialize(using=Base64Serializer.class)` + `@JsonDeserialize(using=Base64Deserializer.class)`
- Add `@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)` to the class
- Add `@JsonSubTypes` listing all 11 concrete subtypes with their element names, e.g.:
  ```java
  @JsonSubTypes({
      @Type(value=BulkheadDTO.class,       name="Bulkhead"),
      @Type(value=CenteringRingDTO.class,  name="CenteringRing"),
      @Type(value=EngineBlockDTO.class,    name="EngineBlock"),
      @Type(value=InnerBodyTubeDTO.class,  name="InnerBodyTube"),
      @Type(value=LaunchLugDTO.class,      name="LaunchLug"),
      @Type(value=NoseConeDTO.class,       name="NoseCone"),
      @Type(value=ParachuteDTO.class,      name="Parachute"),
      @Type(value=RailButtonDTO.class,     name="RailButton"),
      @Type(value=StreamerDTO.class,       name="Streamer"),
      @Type(value=TransitionDTO.class,     name="Transition"),
      @Type(value=TubeCouplerDTO.class,    name="TubeCoupler")
  })
  ```
  *(Verify names match the constants used in the original `@XmlElementRef(name=...)` entries)*

### 3e — `OpenRocketComponentDTO.java`
- Remove `@XmlElementRefs({...})` block entirely (polymorphism now on base class)
- Replace `@XmlElementWrapper(name="Components")` with `@JacksonXmlElementWrapper(localName="Components")`
- Keep `@JacksonXmlRootElement(localName="OpenRocketComponent")` on the class

### 3f — `OpenRocketComponentSaver.java`
- Remove static `JAXBContext CONTEXT` singleton and `private static JAXBContext getContext()`
- Add static `XmlMapper`:
  ```java
  private static final XmlMapper XML_MAPPER =
      (XmlMapper) new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);
  ```
- In `marshalToOpenRocketComponent()`: replace `marshaller.marshal(dto, writer)` with
  `writer.write(XML_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dto))`
- In `unmarshalFromOpenRocketComponent()` / `fromOpenRocketComponent()`: replace
  `(OpenRocketComponentDTO) unmarshaller.unmarshal(is)` with
  `XML_MAPPER.readValue(reader, OpenRocketComponentDTO.class)`
- Change `throws JAXBException` to `throws IOException` in all method signatures
- Remove all `import jakarta.*` lines

### 3g — `OpenRocketComponentLoader.java`
- Replace `catch (JAXBException e)` with `catch (IOException e)` (JacksonException extends IOException)
- Remove `import jakarta.xml.bind.JAXBException`

**Verify Phase 3**: `.\gradlew :core:test --tests "*.OpenRocketComponentSaverTest"`

---

## Phase 4 — `rocksim/export` Package (25 files)
*Marshal only (no read path). Depends on Phase 3 polymorphism pattern.*

### 4a — `BasePartDTO.java`
- Add `@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)` to the class
- Add `@JsonSubTypes` listing all concrete RockSim DTO subtypes with their element name constants
  (e.g., `name=RockSimCommonConstants.BODY_TUBE` for `BodyTubeDTO`, etc.)

### 4b — All 25 DTOs
Apply the annotation mapping table. For DTOs with `@XmlElementRefs` on an attachment list
(`BodyTubeDTO`, `AbstractTransitionDTO`, `StageDTO`):
- Replace `@XmlElementWrapper(name=X) @XmlElementRefs({...})` with `@JacksonXmlElementWrapper(localName=X)` alone
  (polymorphism now handled by `@JsonSubTypes` on `BasePartDTO`)

### 4c — `RockSimSaver.java`
- Remove per-call `JAXBContext.newInstance(RockSimDocumentDTO.class)` and `Marshaller` setup
- Add static `XmlMapper`:
  ```java
  private static final XmlMapper XML_MAPPER = (XmlMapper) new XmlMapper()
      .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, false)
      .enable(SerializationFeature.INDENT_OUTPUT);
  ```
- Replace `marshaller.marshal(dto, sw)` with
  `XML_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dto)`
- Remove all `import jakarta.*` lines

**Verify Phase 4**: `.\gradlew :core:test --tests "*.RockSimDocumentDTOTest"`

---

## Phase 5 — `rasaero/export` Package (18 files)
*Marshal only. Can be done in parallel with Phase 4.*

### 5a — All 18 DTOs
Apply the annotation mapping table. Additionally:
- Replace `@XmlJavaTypeAdapter(CustomDoubleAdapter.class)` → `@JsonSerialize(using=CustomDoubleSerializer.class)`
- Replace `@XmlJavaTypeAdapter(CustomBooleanAdapter.class)` → `@JsonSerialize(using=CustomBooleanSerializer.class)`
- In `TransitionDTO.java`: remove `@XmlSeeAlso({BoattailDTO.class})` — not needed; `BoattailDTO`
  extends `TransitionDTO` and has its own `@JacksonXmlRootElement`

### 5b — `RASAeroSaver.java`
- Same `XmlMapper` pattern as `RockSimSaver` — `WRITE_XML_DECLARATION=false`, `INDENT_OUTPUT`
- Remove all `import jakarta.*` lines

**Verify Phase 5**: `.\gradlew :core:test --tests "*.RASAeroSaverTest"`

---

## Phase 6 — Remove Jakarta Dependencies
*(After phases 3–5 compile and tests pass)*

### `core/build.gradle`
- Remove: `implementation 'jakarta.activation:jakarta.activation-api:2.1.4'`
- Remove: `implementation 'org.glassfish.jaxb:jaxb-runtime:4.0.6'`
- Remove: `implementation 'org.eclipse:yasson:2.0.1'`

### `core/src/main/java/module-info.java`
- Remove: `requires jakarta.xml.bind;`
- Remove: `requires jakarta.activation;`
- Remove: `requires com.sun.istack.runtime;`  ← ships with jaxb-runtime; removing that dep loses this module

### `swing/src/main/java/module-info.java`
- Remove: `requires jakarta.xml.bind;`

**Verify Phase 6**: `.\gradlew :core:compileJava` — no Jakarta references remain

---

## Phase 7 — Swing Cleanup (1 file)

### `swing/src/main/java/info/openrocket/swing/utils/ComponentPresetEditor.java`
- Replace `import jakarta.xml.bind.JAXBException;` with `import com.fasterxml.jackson.core.JacksonException;`
- Update all `catch (JAXBException e)` blocks to `catch (JacksonException e)` (or `IOException`, since
  `JacksonException extends IOException`)

---

## Full Verification

1. `.\gradlew :core:test --tests "*.OpenRocketComponentSaverTest"` — preset read/write round-trip
2. `.\gradlew :core:test --tests "*.RockSimDocumentDTOTest"` — rocksim export XML structure
3. `.\gradlew :core:test --tests "*.RASAeroSaverTest"` — rasaero export XML structure
4. `.\gradlew :core:compileJava` — no Jakarta references remain
5. `.\gradlew :core:test` — full core test suite
6. `.\gradlew run` — launch app, load `.orc` preset file, verify materials/components appear
7. Manual: load a design, export to RockSim (.rkt) and RASAero, open result in a text editor and
   verify XML structure matches pre-migration output

---

## Annotation Mapping Reference

| JAXB (jakarta.xml.bind) | Jackson XML (com.fasterxml.jackson) |
|-------------------------|--------------------------------------|
| `@XmlRootElement(name="X")` | `@JacksonXmlRootElement(localName="X")` |
| `@XmlAccessorType(XmlAccessType.FIELD)` | *(remove — Jackson uses fields by default)* |
| `@XmlElement(name="X")` | `@JacksonXmlProperty(localName="X")` |
| `@XmlAttribute(name="X")` | `@JacksonXmlProperty(localName="X", isAttribute=true)` |
| `@XmlElementWrapper(name="X")` | `@JacksonXmlElementWrapper(localName="X")` |
| `@XmlElementRef` / `@XmlElementRefs` | `@JsonSubTypes` on base class + `@JacksonXmlElementWrapper` on list |
| `@XmlTransient` | `@JsonIgnore` |
| `@XmlType(propOrder={...})` | `@JsonPropertyOrder({...})` |
| `@XmlEnum` | *(remove — Jackson uses enum name by default)* |
| `@XmlEnumValue("X")` | `@JsonProperty("X")` on the enum constant |
| `@XmlSeeAlso({X.class})` | *(remove — handled by `@JsonSubTypes` on base)* |
| `@XmlJavaTypeAdapter(X.class)` | `@JsonSerialize(using=XSerializer.class)` + `@JsonDeserialize(using=XDeserializer.class)` |
| `@XmlInlineBinaryData` | *(remove — handled by custom serializer)* |
| `XmlAdapter<F,B>` | `StdSerializer<T>` + `StdDeserializer<T>` |
| `JAXBContext` / `Marshaller` / `Unmarshaller` | `XmlMapper` |
| `marshaller.marshal(dto, writer)` | `xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto)` |
| `unmarshaller.unmarshal(reader)` | `xmlMapper.readValue(reader, Dto.class)` |
| `Marshaller.JAXB_FRAGMENT = true` | `xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, false)` |
| `Marshaller.JAXB_FORMATTED_OUTPUT = true` | `xmlMapper.enable(SerializationFeature.INDENT_OUTPUT)` |
| `JAXBException` | `JacksonException` (extends `IOException`) |
| `DatatypeConverter.parseBase64Binary()` | `java.util.Base64.getDecoder().decode()` |
| `DatatypeConverter.printBase64Binary()` | `java.util.Base64.getEncoder().encodeToString()` |

---

## Potential Blockers

1. **JPMS for Jackson JARs** — if `jackson-dataformat-xml` (or its dependencies `woodstox`, `stax2-api`)
   are not proper JPMS modules, add `automaticModule('jackson-dataformat-xml-x.x.x.jar', 'com.fasterxml.jackson.dataformat.xml')`
   entries in `extraJavaModuleInfo` in `core/build.gradle`, matching the existing pattern for `de.javagl.obj`.

2. **`com.sun.istack.runtime`** — currently `requires com.sun.istack.runtime;` in `core/module-info.java`.
   This JAR ships with `jaxb-runtime`. Removing `jaxb-runtime` in Phase 6 must include removing this
   `requires` or the module compile will fail.

3. **Polymorphism XML structure** — `WRAPPER_OBJECT` wraps each polymorphic element in a type-name
   wrapper element. Verify the output matches the expected `.orc`/`.rkt` structure by diffing test
   fixture XML before and after migration. If the structure differs, consider `WRAPPER_ARRAY` or a
   custom serializer instead.

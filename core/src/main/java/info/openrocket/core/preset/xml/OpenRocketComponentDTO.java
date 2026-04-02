package info.openrocket.core.preset.xml;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import info.openrocket.core.material.Material;
import info.openrocket.core.preset.ComponentPreset;
import info.openrocket.core.preset.InvalidComponentPresetException;

/**
 * The real 'root' element in an XML document.
 */
@JacksonXmlRootElement(localName = "OpenRocketComponent")
public class OpenRocketComponentDTO {

    @JacksonXmlProperty(localName = "Version")
    private final String version = "0.1";

    @JacksonXmlProperty(localName = "Legacy")
    private String legacy;

    @JacksonXmlElementWrapper(localName = "Materials")
    @JacksonXmlProperty(localName = "Material")
    List<MaterialDTO> materials = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "Components")
    @JacksonXmlProperty(localName = "Component")
    private List<BaseComponentDTO> components = new ArrayList<>();

    public OpenRocketComponentDTO() {
    }

    public OpenRocketComponentDTO(boolean isLegacy, final List<MaterialDTO> theMaterials,
            final List<BaseComponentDTO> theComponents) {
        setLegacy(isLegacy);
        materials = theMaterials;
        components = theComponents;
    }

    public Boolean getLegacy() {
        if (null == legacy) {
            return false;
        }
        return true;
    }

    public void setLegacy(Boolean isLegacy) {
        if (isLegacy) {
            legacy = "";
        } else {
            legacy = null;
        }
    }

    public List<MaterialDTO> getMaterials() {
        return materials;
    }

    public void addMaterial(final MaterialDTO theMaterial) {
        materials.add(theMaterial);
    }

    public void setMaterials(final List<MaterialDTO> theMaterials) {
        materials = theMaterials;
    }

    public List<BaseComponentDTO> getComponents() {
        return components;
    }

    public void addComponent(final BaseComponentDTO theComponent) {
        components.add(theComponent);
    }

    public void setComponents(final List<BaseComponentDTO> theComponents) {
        components = theComponents;
    }

    public List<ComponentPreset> asComponentPresets() throws InvalidComponentPresetException {
        List<ComponentPreset> result = new ArrayList<>(components.size());
		for (BaseComponentDTO component : components) {
			result.add(component.asComponentPreset(getLegacy(), materials));
		}
        return result;
    }

    public List<Material> asMaterialList() {
        List<Material> result = new ArrayList<>(materials.size());
        for (MaterialDTO material : materials) {
            result.add(material.asMaterial());
        }
        return result;
    }
}

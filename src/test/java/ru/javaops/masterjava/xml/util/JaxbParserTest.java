package ru.javaops.masterjava.xml.util;

import com.google.common.io.Resources;
import org.junit.Test;
import ru.javaops.masterjava.xml.schema.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * gkislin
 * 23.09.2016
 */
public class JaxbParserTest {
    private static final JaxbParser JAXB_PARSER = new JaxbParser(ObjectFactory.class);

    static {
        JAXB_PARSER.setSchema(Schemas.ofClasspath("projects.xsd"));
    }

    @Test
    public void testPayload() throws Exception {
//        JaxbParserTest.class.getResourceAsStream("/city.xml")
        Payload payload = JAXB_PARSER.unmarshal(
                Resources.getResource("payload.xml").openStream());
        String strPayload = JAXB_PARSER.marshal(payload);
        JAXB_PARSER.validate(strPayload);
        System.out.println(strPayload);
    }

    @Test
    public void testCity() throws Exception {
        JAXBElement<CityType> cityElement = JAXB_PARSER.unmarshal(
                Resources.getResource("city.xml").openStream());
        CityType city = cityElement.getValue();
        JAXBElement<CityType> cityElement2 =
                new JAXBElement<>(new QName("http://javaops.ru", "City"), CityType.class, city);
        String strCity = JAXB_PARSER.marshal(cityElement2);
        JAXB_PARSER.validate(strCity);
        System.out.println(strCity);
    }

    @Test
    public void testProjectMarshal() throws Exception {
        Projects projects = new Projects();
        projects.setName("test");
        projects.setDesc("test desc");

        Projects.Groups groups = new Projects.Groups();
        List<Group> groupList = groups.getGroup();

        Group group1 = new Group();
        group1.setName("test group");
        group1.setType(GroupType.CURRENT);

        Group group2 = new Group();
        group2.setName("test 2 group");
        group2.setType(GroupType.FINISHED);

        groupList.add(group1);
        groupList.add(group2);

        projects.setGroups(groups);

        JAXBContext jaxbContext = JAXBContext.newInstance(Projects.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);


        jaxbMarshaller.marshal(projects, System.out);
    }
}
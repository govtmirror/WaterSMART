
package gov.usgs.cida.watersmart.parse.file;

import gov.usgs.cida.watersmart.common.ModelType;
import gov.usgs.cida.watersmart.common.RunMetadata;
import gov.usgs.cida.watersmart.parse.CreateDSGFromZip;
import java.io.File;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class PRMS2ParserTest {

    public PRMS2ParserTest() {
    }
    private static final String outputDir = System.getProperty("java.io.tmpdir") + "/prms2";
    private static File sampleFile = null;
    
    @BeforeClass
    public static void setupClass() throws IOException {
        FileUtils.forceMkdir(new File(outputDir));
        sampleFile = new File(PRMS2ParserTest.class.getClassLoader()
                .getResource("gov/usgs/cida/watersmart/netcdf/PRMS_2_realupload.zip")
                .getFile());
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException {
        FileUtils.deleteDirectory(new File(outputDir));
    }

    @Test
    public void testNetCDF() throws IOException, XMLStreamException {
        RunMetadata runMeta = new RunMetadata(ModelType.PRMS2, "1", "test", "1", "1", "2012-07-10T00:00:00Z", 
            "Special", "comments", "jiwalker@usgs.gov", "http://cida-wiwsc-gdp2qa.er.usgs.gov:8082/geoserver/NWC/ows", 
            "NWC:Dense1", "site_no");
        CreateDSGFromZip.ReturnInfo info = CreateDSGFromZip.create(sampleFile, runMeta);
        File ncFile = new File(new File(outputDir), info.filename);
        
        assertThat(info.filename, is(equalTo("PRMS_2_realupload.nc")));
        assertThat(FileUtils.sizeOf(ncFile), is(equalTo(13935321L)));
        FileUtils.deleteQuietly(ncFile);
    }
    
    @Test
    public void testNewSample() throws IOException, XMLStreamException {
        sampleFile = new File(PRMS2ParserTest.class.getClassLoader()
            .getResource("gov/usgs/cida/watersmart/netcdf/PRMS2_sample.zip")
            .getFile());
        
        RunMetadata runMeta = new RunMetadata(ModelType.PRMS2, "1", "test", "1", "1", "2012-07-10T00:00:00Z", 
            "Special", "comments", "jiwalker@usgs.gov", "http://cida-wiwsc-gdp2qa.er.usgs.gov:8082/geoserver/NWC/ows", 
            "NWC:Dense1", "site_no");
        CreateDSGFromZip.ReturnInfo info = CreateDSGFromZip.create(sampleFile, runMeta);
        File ncFile = new File(new File(outputDir), info.filename);
        
        assertThat(info.filename, is(equalTo("PRMS2_sample.nc")));
        assertThat(FileUtils.sizeOf(ncFile), is(equalTo(1101945L)));
        FileUtils.deleteQuietly(ncFile);
    }

}
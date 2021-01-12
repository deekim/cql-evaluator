package org.opencds.cqf.cql.evaluator.library;

import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfig;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class LibraryProcessorTests {

    static LibraryProcessor libraryProcessor = null;
    static FhirContext fhirContext = null;

    @BeforeClass
    @SuppressWarnings("serial")
    public static void setup() {
        fhirContext = FhirContext.forR4();

        AdapterFactory adapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();

        LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);

        Set<TypedLibraryContentProviderFactory> libraryContentProviderFactories = new HashSet<TypedLibraryContentProviderFactory>() {
            {
                add(new TypedLibraryContentProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public LibraryContentProvider create(String url, List<String> headers) {
                        return new BundleFhirLibraryContentProvider(fhirContext,
                                (IBaseBundle) fhirContext.newJsonParser()
                                        .parseResource(LibraryProcessorTests.class.getResourceAsStream(url)),
                                adapterFactory, libraryVersionSelector);
                    }
                });
            }
        };

        Set<ModelResolverFactory> modelResolverFactories = new HashSet<ModelResolverFactory>() {
            {
                add(new FhirModelResolverFactory());
            }
        };

        LibraryLoaderFactory libraryLoaderFactory = new org.opencds.cqf.cql.evaluator.builder.library.LibraryLoaderFactory(
                fhirContext, adapterFactory, libraryContentProviderFactories, libraryVersionSelector);
        Set<TypedRetrieveProviderFactory> retrieveProviderFactories = new HashSet<TypedRetrieveProviderFactory>() {
            {
                add(new TypedRetrieveProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public RetrieveProvider create(String url, List<String> headers) {

                        return new BundleRetrieveProvider(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
                                .parseResource(LibraryProcessorTests.class.getResourceAsStream(url)));
                    }
                });
            }
        };

        DataProviderFactory dataProviderFactory = new org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory(
                fhirContext, modelResolverFactories, retrieveProviderFactories);

        Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories = new HashSet<TypedTerminologyProviderFactory>() {
            {
                add(new TypedTerminologyProviderFactory() {
                    @Override
                    public String getType() {
                        return Constants.HL7_FHIR_FILES;
                    }

                    @Override
                    public TerminologyProvider create(String url, List<String> headers) {
                        return new BundleTerminologyProvider(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
                                .parseResource(LibraryProcessorTests.class.getResourceAsStream(url)));
                    }
                });
            }
        };

        TerminologyProviderFactory terminologyProviderFactory = new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(
                fhirContext, typedTerminologyProviderFactories);

        RetrieveProviderConfigurer retrieveProviderConfigurer = new org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer(
                new RetrieveProviderConfig());

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder(retrieveProviderConfigurer);

        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory()
                .create(fhirContext.getVersion().getVersion());

        CqlFhirParametersConverter cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext,
                adapterFactory, fhirTypeConverter);

        libraryProcessor = new LibraryProcessor(fhirContext, cqlFhirParametersConverter, libraryLoaderFactory,
                dataProviderFactory, terminologyProviderFactory, endpointConverter, cqlEvaluatorBuilder);
    }

    @Test
    public void TestEXM125() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("Numerator").setValue(new BooleanType(true));

        Endpoint endpoint = new Endpoint().setAddress("r4/EXM125-8.0.000-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Set<String> expressions = new HashSet<String>();
        expressions.add("Numerator");

        Parameters actual = (Parameters) libraryProcessor.evaluate(
                new VersionedIdentifier().withId("EXM125").withVersion("8.0.000"), "numer-EXM125", null, endpoint,
                endpoint, endpoint, null, expressions);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void TestRuleFiltersReportable() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("IsReportable").setValue(new BooleanType(true));

        Endpoint endpoint = new Endpoint().setAddress("r4/RuleFilters-1.0.0-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Endpoint dataEndpoint = new Endpoint().setAddress("r4/tests-Reportable-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Set<String> expressions = new HashSet<String>();
        expressions.add("IsReportable");

        Parameters actual = (Parameters) libraryProcessor.evaluate(
                new VersionedIdentifier().withId("RuleFilters").withVersion("1.0.0"), "Reportable", null, endpoint,
                endpoint, dataEndpoint, null, expressions);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void TestRuleFiltersNotReportable() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("IsReportable").setValue(new BooleanType(false));

        Endpoint endpoint = new Endpoint().setAddress("r4/RuleFilters-1.0.0-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Endpoint dataEndpoint = new Endpoint().setAddress("r4/tests-NotReportable-bundle.json")
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        Set<String> expressions = new HashSet<String>();
        expressions.add("IsReportable");

        Parameters actual = (Parameters) libraryProcessor.evaluate(
                new VersionedIdentifier().withId("RuleFilters").withVersion("1.0.0"), "NotReportable", null, endpoint,
                endpoint, dataEndpoint, null, expressions);

        assertTrue(expected.equalsDeep(actual));
    }
}
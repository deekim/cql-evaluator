package org.opencds.cqf.cql.evaluator.cql2elm;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AttachmentAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.LibraryAdapter;

public abstract class VersionComparingLibrarySourceProvider
        implements LibrarySourceProvider {

    private AdapterFactory adapterFactory;

    public VersionComparingLibrarySourceProvider(AdapterFactory adapterFactory) {
        this.adapterFactory = requireNonNull(adapterFactory, "adapterFactory can not be null");
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        requireNonNull(versionedIdentifier, "versionedIdentifier can not be null.");

        IBaseResource library = this.getLibrary(versionedIdentifier);
        if (library == null) {
            return null;
        }

        return this.getCqlStream(library);
    }

    private InputStream getCqlStream(IBaseResource library) {

        LibraryAdapter libraryAdapter = this.adapterFactory.createLibrary(library);
        
        if (libraryAdapter.hasContent()) {
            for (ICompositeType attachment : libraryAdapter.getContent()) {
                AttachmentAdapter attachmentAdapter = this.adapterFactory.createAttachment(attachment);
                if (attachmentAdapter.getContentType().equals("text/cql")) {
                    return new ByteArrayInputStream(attachmentAdapter.getData());
                }
            }
        }

        return null;
    }

    protected abstract IBaseResource getLibrary(VersionedIdentifier libraryIdentifier);

    protected IBaseResource select(VersionedIdentifier libraryIdentifier, Collection<IBaseResource> libraries) {
        requireNonNull(libraries, "libraries can not be null");
        requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");

        String targetVersion = libraryIdentifier.getVersion();

        List<LibraryAdapter> adapters = libraries.stream().map(x -> this.adapterFactory.createLibrary(x)).collect(Collectors.toList());

        LibraryAdapter library = null;
        LibraryAdapter maxLibrary = null;

        for (LibraryAdapter l : adapters) {

            if (!l.getName().equals(libraryIdentifier.getId())) {
                continue;
            }

            String currentVersion = l.getVersion();
            if ((targetVersion != null && currentVersion != null && currentVersion.equals(targetVersion)) || 
                (targetVersion == null && currentVersion == null)) {
                library = l;
            }

            if (maxLibrary == null || compareVersions(maxLibrary.getVersion(), currentVersion) < 0) {
                maxLibrary = l;
            }
        }

        if (targetVersion == null && maxLibrary != null) {
            library = maxLibrary;
        }

        return library.get();
    }

    public static int compareVersions(String version1, String version2) {
        // Treat null as MAX VERSION
        if (version1 == null && version2 == null) {
            return 0;
        }

        if (version1 != null && version2 == null) {
            return -1;
        }

        if (version1 == null && version2 != null) {
            return 1;
        }

        String[] string1Values = version1.split("\\.");
        String[] string2Values = version2.split("\\.");

        int length = Math.max(string1Values.length, string2Values.length);

        for (int i = 0; i < length; i++) {
            Integer v1 = (i < string1Values.length) ? Integer.parseInt(string1Values[i]) : 0;
            Integer v2 = (i < string2Values.length) ? Integer.parseInt(string2Values[i]) : 0;

            // Making sure Version1 bigger than version2
            if (v1 > v2) {
                return 1;
            }
            // Making sure Version1 smaller than version2
            else if (v1 < v2) {
                return -1;
            }
        }

        // Both are equal
        return 0;
    }
}
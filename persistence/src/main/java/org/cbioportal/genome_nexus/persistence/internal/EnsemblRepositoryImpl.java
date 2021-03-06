package org.cbioportal.genome_nexus.persistence.internal;

import org.cbioportal.genome_nexus.model.EnsemblTranscript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import org.springframework.stereotype.Repository;

@Repository
public class EnsemblRepositoryImpl implements EnsemblRepositoryCustom
{
    private final MongoTemplate mongoTemplate;

    @Autowired
    public EnsemblRepositoryImpl(MongoTemplate mongoTemplate)
    {
        this.mongoTemplate = mongoTemplate;
    }

    public static final String CANONICAL_TRANSCRIPTS_COLLECTION = "ensembl.canonical_transcript_per_hgnc";
    public static final String TRANSCRIPTS_COLLECTION = "ensembl.biomart_transcripts";

    @Override
    public EnsemblTranscript findOneByHugoSymbolIgnoreCase(String hugoSymbol, String isoformOverrideSource) {
        BasicDBObject regexQuery = new BasicDBObject();
        // case insensitive exact match query
        regexQuery.put("hgnc_symbol",
            new BasicDBObject("$regex", "^" + hugoSymbol + "$")
                .append("$options", "i"));

        Cursor transcriptCursor;
        Cursor canonicalCursor = mongoTemplate.getCollection(CANONICAL_TRANSCRIPTS_COLLECTION).find(regexQuery);

        if (canonicalCursor.hasNext()) {
            BasicDBObject canonicalTranscriptsPerSource = (BasicDBObject) canonicalCursor.next();

            String transcriptId = (String) canonicalTranscriptsPerSource.get(isoformOverrideSource + "_canonical_transcript");

            BasicDBObject whereQuery = new BasicDBObject();
            whereQuery.put(EnsemblTranscript.TRANSCRIPT_ID_FIELD_NAME, transcriptId);

            transcriptCursor = mongoTemplate.getCollection(TRANSCRIPTS_COLLECTION).find(whereQuery);
            if (transcriptCursor.hasNext()) {
               EnsemblTranscript transcript = mongoTemplate.getConverter().read(EnsemblTranscript.class, transcriptCursor.next());
                if (transcript != null) {
                    return transcript;
                }
            }
        }
        return null;
    }
}

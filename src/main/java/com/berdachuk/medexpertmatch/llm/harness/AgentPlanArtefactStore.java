package com.berdachuk.medexpertmatch.llm.harness;

import java.util.Optional;

public interface AgentPlanArtefactStore {

    void save(AgentPlanArtefact artefact);

    Optional<AgentPlanArtefact> findBySessionId(String sessionId);
}

/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.cmmn.test.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnRepositoryService;
import org.flowable.cmmn.engine.impl.CmmnEngineImpl;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.repository.CaseDefinition;
import org.flowable.cmmn.engine.repository.CmmnDeployment;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.engine.common.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.engine.common.impl.persistence.deploy.DeploymentCache;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class DeploymentTest {
    
    private CmmnEngine cmmnEngine;
    private CmmnRepositoryService cmmnRepositoryService;
    
    @Before
    public void setupCmmnEngine() {
        CmmnEngineConfiguration config = new StandaloneInMemCmmnEngineConfiguration();
        config.setDatabaseSchemaUpdate(CmmnEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP);
        this.cmmnEngine = config.build();
        this.cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
    }
    
    /**
     * Simplest test possible: deploy the simple-case.cmmn (from the cmmn-converter module) and see if 
     * - a deployment exists
     * - a resouce exists
     * - a case definition was created 
     * - that case definition is in the cache
     * - case definition properties set
     */
    @Test
    public void testCaseDefinitionDeployed() throws Exception {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/repository/simple-case.cmmn")
            .deploy();
        assertNotNull(cmmnDeployment);
        
        List<String> resourceNames = cmmnRepositoryService.getDeploymentResourceNames(cmmnDeployment.getId());
        assertEquals(1, resourceNames.size());
        assertEquals("org/flowable/cmmn/test/repository/simple-case.cmmn", resourceNames.get(0));
        
        InputStream inputStream = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.getId(), resourceNames.get(0));
        assertNotNull(inputStream);
        inputStream.close();
        
        // TODO: next steps: add query capabilities for deployment / case definition
        
        DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache = ((CmmnEngineImpl) cmmnEngine).getCmmnEngineConfiguration().getCaseDefinitionCache();
        assertEquals(1, ((DefaultDeploymentCache<CaseDefinitionCacheEntry>) caseDefinitionCache).getAll().size());
        
        CaseDefinitionCacheEntry cachedCaseDefinition = ((DefaultDeploymentCache<CaseDefinitionCacheEntry>) caseDefinitionCache).getAll().iterator().next();
        assertNotNull(cachedCaseDefinition.getCase());
        assertNotNull(cachedCaseDefinition.getCmmnModel());
        assertNotNull(cachedCaseDefinition.getCaseDefinition());
        
        CaseDefinition caseDefinition = cachedCaseDefinition.getCaseDefinition();
        assertNotNull(caseDefinition.getId());
        assertNotNull(caseDefinition.getDeploymentId());
        assertNotNull(caseDefinition.getKey());
        assertNotNull(caseDefinition.getResourceName());
        assertTrue(caseDefinition.getVersion() > 0);
        
        CmmnModel cmmnModel = cmmnRepositoryService.getCmmnModel(caseDefinition.getId());
        assertNotNull(cmmnModel);
        
        // CmmnParser should have added behavior to plan items
        for (PlanItem planItem : cmmnModel.getPrimaryCase().getPlanModel().getPlanItems()) {
            assertNotNull(planItem.getBehavior());
        }
    }

}
/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.unifiedpush.jpa.dao.impl;


import org.jboss.aerogear.unifiedpush.api.PushMessageInformation;
import org.jboss.aerogear.unifiedpush.dao.PageResult;
import org.jboss.aerogear.unifiedpush.dao.PushMessageInformationDao;
import org.jboss.aerogear.unifiedpush.utils.AeroGearLogger;

import java.util.Date;
import java.util.List;


public class JPAPushMessageInformationDao extends JPABaseDao implements PushMessageInformationDao {

    private static final String ASC = "ASC";
    private static final String DESC = "DESC";

    private final AeroGearLogger logger = AeroGearLogger.getInstance(JPAPushMessageInformationDao.class);


    @Override
    public List<PushMessageInformation> findAllForPushApplication(String pushApplicationId, boolean ascending) {
        return createQuery("select pmi from PushMessageInformation pmi where pmi.pushApplicationId = :pushApplicationId ORDER BY pmi.submitDate " + ascendingOrDescending(ascending))
                .setParameter("pushApplicationId", pushApplicationId).getResultList();
    }

    @Override
    public PageResult<PushMessageInformation> findAllForPushApplication(String pushApplicationId, boolean ascending, Integer page, Integer pageSize) {
        final String query = "select pmi from PushMessageInformation pmi where pmi.pushApplicationId = :pushApplicationId ORDER BY pmi.submitDate " + ascendingOrDescending(ascending);
        final String countQuery = "select count(*) from PushMessageInformation pmi where pmi.pushApplicationId = :pushApplicationId";
        return executePagedQuery(pushApplicationId, "pushApplicationId", page, pageSize, query, countQuery);
    }

    @Override
    //TODO sub optimal fetch join doesn't work well with min max all results will get fetched and min max will be in memory
    public PageResult<PushMessageInformation> findAllForVariant(String variantID, boolean ascending, Integer page, Integer pageSize) {
        final String query = "select pmi from PushMessageInformation pmi JOIN fetch pmi.variantInformations vi where vi.variantID = :variantID ORDER BY pmi.submitDate " + ascendingOrDescending(ascending);
        final String countQuery = "select count(*) from PushMessageInformation pmi JOIN pmi.variantInformations vi where vi.variantID = :variantID";
        return executePagedQuery(variantID, "variantID", page, pageSize, query, countQuery);
    }

    private PageResult<PushMessageInformation> executePagedQuery(String param, String paramName, Integer page, Integer pageSize, String query, String countQuery) {
        List<PushMessageInformation> pushMessageInformationList = createQuery(query)
                .setParameter(paramName, param)
                .setFirstResult(page * pageSize).setMaxResults(pageSize).getResultList();

        Long count = (Long) createQuery(countQuery).setParameter(paramName, param).getSingleResult();

        return new PageResult<PushMessageInformation>(pushMessageInformationList, count);
    }

    @Override
    public long getNumberOfPushMessagesForApplications(String loginName) {
        return (Long) createQuery("select count(pmi) from PushMessageInformation pmi where pmi.pushApplicationId " +
                "IN (select p.pushApplicationID from PushApplication p where p.developer = :developer)")
                .setParameter("developer", loginName).getSingleResult();
    }

    @Override
    public PushMessageInformation find(String id) {
        return entityManager.find(PushMessageInformation.class, id);
    }

    @Override
    public void create(PushMessageInformation pushMessageInformation) {
        persist(pushMessageInformation);
    }

    @Override
    public void update(PushMessageInformation pushMessageInformation) {
        merge(pushMessageInformation);
    }

    @Override
    public void delete(PushMessageInformation pushMessageInformation) {
        PushMessageInformation entity = find(pushMessageInformation.getId());
        remove(entity);
    }

    @Override
    public List<String> findVariantIDsWithWarnings(String loginName) {
        return createQuery("select distinct vmi.variantID from VariantMetricInformation vmi" +
                " where vmi.variantID IN (select t.variantID from Variant t where t.developer = :developer)" +
                " and vmi.deliveryStatus = false")
                .setParameter("developer", loginName)
                .getResultList();
    }

    @Override
    public List<PushMessageInformation> findLastThreeActivity(String loginName) {
        return createQuery("select pmi from PushMessageInformation pmi where pmi.pushApplicationId" +
                " IN (select p.pushApplicationID from PushApplication p where p.developer = :developer)" +
                " ORDER BY pmi.submitDate " + DESC)
                .setParameter("developer", loginName)
                .setMaxResults(3)
                .getResultList();
    }

    @Override
    public void deletePushInformationOlderThan(Date oldest) {
        // TODO: use criteria API...
        List<PushMessageInformation> oldMessages = createQuery("select pmi FROM PushMessageInformation pmi WHERE pmi.submitDate < :oldest")
        .setParameter("oldest", oldest).getResultList();

        logger.info("Deleting ['" + oldMessages.size() + "'] outdated PushMessageInformation objects");

        for (PushMessageInformation oldMessage : oldMessages) {
            remove(oldMessage);
        }
    }

    //Admin queries
    @Override
    public List<String> findVariantIDsWithWarnings() {
        return createQuery("select distinct vmi.variantID from VariantMetricInformation vmi" +
                " where vmi.variantID IN (select t.variantID from Variant t)" +
                " and vmi.deliveryStatus = false")
                .getResultList();
    }

    @Override
    public List<PushMessageInformation> findLastThreeActivity() {
        return createQuery("select pmi from PushMessageInformation pmi where pmi.pushApplicationId" +
                " IN (select p.pushApplicationID from PushApplication p)" +
                " ORDER BY pmi.submitDate " + DESC)
                .setMaxResults(3)
                .getResultList();
    }

    @Override
    public long getNumberOfPushMessagesForApplications() {
        return (Long) createQuery("select count(pmi) from PushMessageInformation pmi where pmi.pushApplicationId " +
                "IN (select p.pushApplicationID from PushApplication p)").getSingleResult();
    }


    /**
     * Helper that returns 'ASC' when true and 'DESC' when false.
     */
    private String ascendingOrDescending(boolean asc) {
        if (asc) {
            return ASC;
        } else {
            return DESC;
        }
    }
}

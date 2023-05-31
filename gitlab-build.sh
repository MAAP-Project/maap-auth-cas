#!/usr/bin/env bash

IMAGE_NAME="$CI_REGISTRY_IMAGE/maap-auth-cas:latest"
CAS_PROPS="cas/etc/cas/config/cas.properties"

pushd maap-auth-cas

# Update cas.properties with CI config variables
echo "" >> ${CAS_PROPS}

echo "cas.authn.pac4j.oauth2[0].id=${URS_KEY}" >> ${CAS_PROPS}
echo "cas.authn.pac4j.oauth2[0].custom-params.urs4_key=${URS_KEY}" >> ${CAS_PROPS}
echo "cas.authn.pac4j.oauth2[0].secret=${URS_SECRET}" >> ${CAS_PROPS}
echo "cas.authn.pac4j.oauth2[0].profile-attrs.cas_key=${CAS_SECRET_KEY}" >> ${CAS_PROPS}
echo "cas.authn.pac4j.oidc[0].generic.id=${ESA_ID}" >> ${CAS_PROPS}
echo "cas.authn.pac4j.oidc[0].generic.secret=${ESA_SECRET}" >> ${CAS_PROPS}
echo "cas.authn.pac4j.oidc[1].generic.id=${ESA_API_ID}" >> ${CAS_PROPS}
echo "cas.authn.pac4j.oidc[1].generic.secret=${ESA_API_SECRET}" >> ${CAS_PROPS}

sed -i "s/maap-environment/$CAS_SERVER_NAME/g" ${CAS_PROPS}
sed -i "s/cas-delegated-urs-name/$CAS_DELEGATED_URS_NAME/g" ${CAS_PROPS}

# Update service definitions with CI config variables
sed -i "s/maap-environment/$CAS_SERVER_NAME/g" $(find cas/etc/cas/services-repo/ -type f)
sed -i "s/clientSecretValue/$ADE_CLIENT_SECRET/g" "cas/etc/cas/services-repo/NASA_ADE-00002.json"
sed -i "s/clientIdValue/$ADE_CLIENT_ID/g" "cas/etc/cas/services-repo/NASA_ADE-00002.json"
sed -i "s/clientSecretValue/$OAUTH_CLIENT_SECRET/g" "cas/etc/cas/services-repo/NASA_OAuth-33443.json"
sed -i "s/clientIdValue/$OAUTH_CLIENT_ID/g" "cas/etc/cas/services-repo/NASA_OAuth-33443.json"
sed -i "s/clientSecretValue/$ESA_ADE_CLIENT_SECRET/g" "cas/etc/cas/services-repo/ESA_ADE-00020.json"
sed -i "s/clientIdValue/$ESA_ADE_CLIENT_ID/g" "cas/etc/cas/services-repo/ESA_ADE-00020.json"
sed -i "s/clientIdValue/$ESA_LIFERAY_CLIENT_ID/g" "cas/etc/cas/services-repo/ESA_Liferay-00025.json"

sed -i "s/casDockerImage/mas.$CAS_SERVER_NAME.maap-project.org\/root\/auth-ci\/maap-auth-cas/g" "docker-compose-ci.yml"

git log -1 > commit.txt
chmod 640 ../*.key
cp -v ../*.key cas/etc/cas/

chmod 640 ../*.jwks
cp -v ../*.jwks cas/etc/cas/
cp -v ../*.jwks cas/etc/cas/config/

docker-compose -f docker-compose-ci.yml build 
docker push ${IMAGE_NAME}


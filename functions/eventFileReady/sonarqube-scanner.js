const options = {
    "sonar.organization": "pagopa",
    "sonar.projectKey": "pagopa_pn-portfat-EventFileReady",
};

if (process.env.PR_NUM) {
    options["sonar.pullrequest.base"] = process.env.BRANCH_TARGET;
    options["sonar.pullrequest.branch"] = process.env.BRANCH_NAME;
    options["sonar.pullrequest.key"] = process.env.PR_NUM;
}

const scanner = require("sonarqube-scanner");

(async () => {
    try {
        await scanner({
                serverUrl: "https://sonarcloud.io",
                options: options,
            },
            () => {
                console.log("SonarQube Scanner execution completed.");
                process.exit(0);
            });

        process.exit(0);
    } catch (error) {
        console.error("Error during SonarQube Scanner execution:", error);
        process.exit(1);
    }
})();
const scanner = require("sonarqube-scanner");

const options = {
    "sonar.organization": "pagopa",
    "sonar.projectKey": "pagopa_pn-portfat-EventFileReady",
};

if (process.env.PR_NUM) {
    options["sonar.pullrequest.base"] = process.env.BRANCH_TARGET;
    options["sonar.pullrequest.branch"] = process.env.BRANCH_NAME;
    options["sonar.pullrequest.key"] = process.env.PR_NUM;
}

(async () => {
    try {
        await new Promise((resolve, reject) => {
            scanner(
                {
                    serverUrl: "https://sonarcloud.io",
                    options: options,
                },
                (error) => {
                    if (error) {
                        reject(error);
                    } else {
                        console.log("SonarQube Scanner execution completed.");
                        resolve();
                    }
                }
            );
        });
        process.exit(0);
    } catch (error) {
        console.error("SonarQube Scanner failed:", error);
        process.exit(1);
    }
})();
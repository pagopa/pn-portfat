const assert = require("assert");
const sinon = require("sinon");
const proxyquire = require("proxyquire").noCallThru();
const { YearMonth } = require("@js-joda/core");

describe("Monthly records check lambda", () => {
    let handler;
    let getCompletedRecordsByMonthSentStub;
    let yearMonthNowStub;
    let consoleLogStub;
    let consoleErrorStub;

    beforeEach(() => {
        /*
         * Mock Dynamo dependency used by eventHandler:
         *
         * src/app/eventHandler.js
         *      -> ./lib/dynamo
         */
        getCompletedRecordsByMonthSentStub = sinon.stub();

        ({ handler } = proxyquire("../app/eventHandler", {
            "./lib/dynamo": {
                getCompletedRecordsByMonthSent:
                getCompletedRecordsByMonthSentStub
            }
        }));

        /*
         * Fixed current month:
         *
         * X   = 2026-10
         * X+1 = 2026-11
         * X+4 = 2027-02
         */
        yearMonthNowStub = sinon
            .stub(YearMonth, "now")
            .returns(YearMonth.parse("2026-10"));

        consoleLogStub = sinon
            .stub(console, "log");

        consoleErrorStub = sinon
            .stub(console, "error");
    });

    afterEach(() => {
        sinon.restore();
    });

    describe("successful checks", () => {
        it("should return OK when both X+1 and X+4 records exist", async () => {
            const records = [
                {
                    orderMonth: "2026-11"
                },
                {
                    orderMonth: "2027-02"
                }
            ];

            getCompletedRecordsByMonthSentStub.resolves(records);

            const result = await handler();

            sinon.assert.calledOnce(yearMonthNowStub);

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );

            assert.deepStrictEqual(result, {
                checkResult: "OK",
                missingMonths: []
            });

            sinon.assert.calledWith(
                consoleLogStub,
                "Starting monthly records check lambda"
            );

            sinon.assert.calledWith(
                consoleLogStub,
                "Current monthSent: 2026-10"
            );

            sinon.assert.calledWith(
                consoleLogStub,
                "Expected months: X+1=2026-11, X+4=2027-02"
            );

            sinon.assert.calledWith(
                consoleLogStub,
                "Found 2 COMPLETED records for monthSent=2026-10"
            );

            sinon.assert.calledWith(
                consoleLogStub,
                "Monthly records check passed"
            );

            sinon.assert.notCalled(consoleErrorStub);
        });

        it("should return OK when multiple records exist for X+1 and X+4", async () => {
            const records = [
                {
                    id: "1",
                    orderMonth: "2026-11"
                },
                {
                    id: "2",
                    orderMonth: "2026-11"
                },
                {
                    id: "3",
                    orderMonth: "2027-02"
                },
                {
                    id: "4",
                    orderMonth: "2027-02"
                }
            ];

            getCompletedRecordsByMonthSentStub.resolves(records);

            const result = await handler();

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );

            assert.deepStrictEqual(result, {
                checkResult: "OK",
                missingMonths: []
            });

            sinon.assert.calledWith(
                consoleLogStub,
                "Found 4 COMPLETED records for monthSent=2026-10"
            );

            sinon.assert.calledWith(
                consoleLogStub,
                "Monthly records check passed"
            );

            sinon.assert.notCalled(consoleErrorStub);
        });

        it("should return OK when expected records exist together with unrelated records", async () => {
            const records = [
                {
                    id: "1",
                    orderMonth: "2026-09"
                },
                {
                    id: "2",
                    orderMonth: "2026-11"
                },
                {
                    id: "3",
                    orderMonth: "2026-12"
                },
                {
                    id: "4",
                    orderMonth: "2027-01"
                },
                {
                    id: "5",
                    orderMonth: "2027-02"
                },
                {
                    id: "6",
                    orderMonth: "2030-01"
                }
            ];

            getCompletedRecordsByMonthSentStub.resolves(records);

            const result = await handler();

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );

            assert.deepStrictEqual(result, {
                checkResult: "OK",
                missingMonths: []
            });

            sinon.assert.calledWith(
                consoleLogStub,
                "Found 6 COMPLETED records for monthSent=2026-10"
            );

            sinon.assert.notCalled(consoleErrorStub);
        });
    });

    describe("failed checks", () => {
        it("should return KO when X+1 record is missing", async () => {
            const records = [
                {
                    orderMonth: "2027-02"
                }
            ];

            getCompletedRecordsByMonthSentStub.resolves(records);

            const result = await handler();

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );

            assert.deepStrictEqual(result, {
                checkResult: "KO",
                missingMonths: ["X+1 (2026-11)"]
            });

            assert.strictEqual(
                consoleLogStub.calledWith(
                    "Monthly records check passed"
                ),
                false
            );

            sinon.assert.calledOnce(consoleErrorStub);

            sinon.assert.calledWithExactly(
                consoleErrorStub,
                "Missing records for: X+1 (2026-11)"
            );
        });

        it("should return KO when X+4 record is missing", async () => {
            const records = [
                {
                    orderMonth: "2026-11"
                }
            ];

            getCompletedRecordsByMonthSentStub.resolves(records);

            const result = await handler();

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );

            assert.deepStrictEqual(result, {
                checkResult: "KO",
                missingMonths: ["X+4 (2027-02)"]
            });

            assert.strictEqual(
                consoleLogStub.calledWith(
                    "Monthly records check passed"
                ),
                false
            );

            sinon.assert.calledOnce(consoleErrorStub);

            sinon.assert.calledWithExactly(
                consoleErrorStub,
                "Missing records for: X+4 (2027-02)"
            );
        });

        it("should return KO when both X+1 and X+4 records are missing", async () => {
            const records = [
                {
                    orderMonth: "2026-12"
                },
                {
                    orderMonth: "2027-01"
                }
            ];

            getCompletedRecordsByMonthSentStub.resolves(records);

            const result = await handler();

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );

            assert.deepStrictEqual(result, {
                checkResult: "KO",
                missingMonths: [
                    "X+1 (2026-11)",
                    "X+4 (2027-02)"
                ]
            });

            sinon.assert.calledOnce(consoleErrorStub);

            sinon.assert.calledWithExactly(
                consoleErrorStub,
                "Missing records for: X+1 (2026-11), X+4 (2027-02)"
            );

            assert.strictEqual(
                consoleLogStub.calledWith(
                    "Monthly records check passed"
                ),
                false
            );
        });

        it("should return KO when DynamoDB returns an empty array", async () => {
            getCompletedRecordsByMonthSentStub.resolves([]);

            const result = await handler();

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );

            assert.deepStrictEqual(result, {
                checkResult: "KO",
                missingMonths: [
                    "X+1 (2026-11)",
                    "X+4 (2027-02)"
                ]
            });

            sinon.assert.calledWith(
                consoleLogStub,
                "Found 0 COMPLETED records for monthSent=2026-10"
            );

            sinon.assert.calledWithExactly(
                consoleErrorStub,
                "Missing records for: X+1 (2026-11), X+4 (2027-02)"
            );
        });

        it("should ignore records without orderMonth", async () => {
            const records = [
                {
                    id: "1"
                },
                {
                    id: "2",
                    orderMonth: null
                },
                {
                    id: "3",
                    orderMonth: undefined
                },
                {
                    id: "4",
                    orderMonth: "2026-11"
                }
            ];

            getCompletedRecordsByMonthSentStub.resolves(records);

            const result = await handler();

            assert.deepStrictEqual(result, {
                checkResult: "KO",
                missingMonths: ["X+4 (2027-02)"]
            });

            sinon.assert.calledWithExactly(
                consoleErrorStub,
                "Missing records for: X+4 (2027-02)"
            );
        });

        it("should use exact orderMonth match", async () => {
            const records = [
                {
                    orderMonth: "2026-11-extra"
                },
                {
                    orderMonth: "2027-02-extra"
                },
                {
                    orderMonth: "2026-11 "
                },
                {
                    orderMonth: " 2027-02"
                }
            ];

            getCompletedRecordsByMonthSentStub.resolves(records);

            const result = await handler();

            assert.deepStrictEqual(result, {
                checkResult: "KO",
                missingMonths: [
                    "X+1 (2026-11)",
                    "X+4 (2027-02)"
                ]
            });
        });
    });

    describe("date calculation", () => {
        it("should correctly calculate months when crossing the year boundary", async () => {
            yearMonthNowStub.returns(
                YearMonth.parse("2026-12")
            );

            /*
             * X   = 2026-12
             * X+1 = 2027-01
             * X+4 = 2027-04
             */
            getCompletedRecordsByMonthSentStub.resolves([
                {
                    orderMonth: "2027-01"
                },
                {
                    orderMonth: "2027-04"
                }
            ]);

            const result = await handler();

            sinon.assert.calledOnce(yearMonthNowStub);

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-12"
            );

            assert.deepStrictEqual(result, {
                checkResult: "OK",
                missingMonths: []
            });

            sinon.assert.calledWith(
                consoleLogStub,
                "Current monthSent: 2026-12"
            );

            sinon.assert.calledWith(
                consoleLogStub,
                "Expected months: X+1=2027-01, X+4=2027-04"
            );
        });

        it("should correctly report missing months when crossing the year boundary", async () => {
            yearMonthNowStub.returns(
                YearMonth.parse("2026-12")
            );

            getCompletedRecordsByMonthSentStub.resolves([]);

            const result = await handler();

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-12"
            );

            assert.deepStrictEqual(result, {
                checkResult: "KO",
                missingMonths: [
                    "X+1 (2027-01)",
                    "X+4 (2027-04)"
                ]
            });

            sinon.assert.calledWithExactly(
                consoleErrorStub,
                "Missing records for: X+1 (2027-01), X+4 (2027-04)"
            );
        });
    });

    describe("DynamoDB errors", () => {
        it("should propagate the error when getCompletedRecordsByMonthSent rejects", async () => {
            const dynamoError =
                new Error("DynamoDB unavailable");

            getCompletedRecordsByMonthSentStub.rejects(
                dynamoError
            );

            let caughtError;

            try {
                await handler();
            } catch (error) {
                caughtError = error;
            }

            assert.ok(caughtError);

            assert.strictEqual(
                caughtError.message,
                "DynamoDB unavailable"
            );

            sinon.assert.calledOnce(yearMonthNowStub);

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );

            /*
             * Execution stops at the Dynamo call, therefore the records
             * count and final result are never logged.
             */
            const foundLog = consoleLogStub
                .getCalls()
                .some(
                    (call) =>
                        typeof call.args[0] === "string" &&
                        call.args[0].includes("Found ")
                );

            assert.strictEqual(
                foundLog,
                false
            );

            assert.strictEqual(
                consoleLogStub.calledWith(
                    "Monthly records check passed"
                ),
                false
            );

            sinon.assert.notCalled(consoleErrorStub);
        });

        it("should propagate the exact error returned by DynamoDB", async () => {
            const dynamoError =
                new Error("Query failed");

            dynamoError.name =
                "ProvisionedThroughputExceededException";

            getCompletedRecordsByMonthSentStub.rejects(
                dynamoError
            );

            let caughtError;

            try {
                await handler();
            } catch (error) {
                caughtError = error;
            }

            assert.strictEqual(
                caughtError,
                dynamoError
            );

            sinon.assert.calledOnce(
                getCompletedRecordsByMonthSentStub
            );

            sinon.assert.calledWithExactly(
                getCompletedRecordsByMonthSentStub,
                "2026-10"
            );
        });
    });
});
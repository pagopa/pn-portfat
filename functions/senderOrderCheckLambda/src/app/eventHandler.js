const { YearMonth } = require('@js-joda/core');
const {getCompletedRecordsByMonthSent} = require('./dynamo');

exports.handler = async function handler() {
    console.log('Starting monthly records check lambda');

    // Current month in yyyy-MM format
    const currentMonth = YearMonth.now();
    const monthSent = currentMonth.toString();
    const nextMonth = currentMonth.plusMonths(1).toString();
    const fourthMonth = currentMonth.plusMonths(4).toString();

    console.log(`Current monthSent: ${monthSent}`);
    console.log(`Expected months: X+1=${nextMonth}, X+4=${fourthMonth}`);

    const records = await getCompletedRecordsByMonthSent(monthSent);

    console.log(`Found ${records.length} COMPLETED records for monthSent=${monthSent}`);

    /*
     * Check that at least one record exists for X+1.
     * Multiple records for the same month are allowed.
     */
    const hasNextMonthRecord = records.some(record => record.orderMonth === nextMonth);

    /*
     * Check that at least one record exists for X+4.
     * Multiple records for the same month are allowed.
     */
    const hasFourthMonthRecord = records.some(record => record.orderMonth === fourthMonth);

    /*
     * The check is successful only if both
     * expected months are present.
     */
    const missingMonths = [];

    if (!hasNextMonthRecord) {
        missingMonths.push(`X+1 (${nextMonth})`);
    }

    if (!hasFourthMonthRecord) {
        missingMonths.push(`X+4 (${fourthMonth})`);
    }

    const isCheckPassed = missingMonths.length === 0;

    if (isCheckPassed) {
        console.log('Monthly records check passed');
    } else {
        console.error(`Missing records for: ${missingMonths.join(', ')}`);
    }

    return {
        checkResult: isCheckPassed ? 'OK' : 'KO',
        missingMonths
    };
};
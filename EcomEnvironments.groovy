package jobs.ecom

/**
 *
 * @author lkishalmi
 */
class EcomEnvironments {

    public static Map<String, List<String>> REGIONAL_ENVIRONMENTS = [
        'au' : ['dev3', 'dev3dm', 'dev3so', 'dev3up', 'tst4', 'tst6', 'tst9', 'ppd3', 'prod'],
        'jp' : ['dev3', 'dev3up', 'tst4', 'tst6','tst9', 'ppd3', 'prod'],
        'us' : ['dev3', 'dev3dm', 'dev3so', 'dev3up', 'dev4', 'tst4', 'tst6', 'tst9', 'ppd3', 'prod', 'ppd3dr'],
    ]
}


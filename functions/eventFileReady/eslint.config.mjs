import globals from "globals";
import pluginJs from "@eslint/js";


export default [
    { ignores: ["dist/*"] },
    { files: ["**/*.{js,mjs,cjs,ts}"] },
    { files: ["**/*.js"], languageOptions: { sourceType: "commonjs" } },
    {
        files: ["src/**/*.js"],
        languageOptions: {
            globals: {
                ...globals.node
            }
        }
    },
    {
        files: ["src/test/**/*.js"],
        languageOptions: {
            globals: {
                ...globals.mocha
            }
        }
    },
    pluginJs.configs.recommended,
];
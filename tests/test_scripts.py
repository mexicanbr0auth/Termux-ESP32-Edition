import pathlib
import subprocess
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
CLI = ROOT / "scripts" / "esp32.sh"


class ScriptTests(unittest.TestCase):
    def run_cli(self, *args):
        return subprocess.run(
            ["bash", str(CLI), *args],
            text=True,
            capture_output=True,
            timeout=10,
        )

    def test_help(self):
        result = self.run_cli("help")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("doctor", result.stdout)
        self.assertIn("bridge-test", result.stdout)

    def test_unknown_command_fails(self):
        result = self.run_cli("not-a-command")
        self.assertEqual(result.returncode, 2)
        self.assertIn("Unknown command", result.stderr)

    def test_flash_requires_arguments(self):
        result = self.run_cli("flash-mpy")
        self.assertEqual(result.returncode, 2)
        self.assertIn("Usage:", result.stderr)


if __name__ == "__main__":
    unittest.main()

from pathlib import Path
import tempfile
import unittest
from check_architecture import violations

class ArchitectureCheckerTests(unittest.TestCase):
    def check(self, controller, service=''):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            java=root/'backend/src/main/java/com/reservedbytes/cinema_reservation'
            (java/'controller').mkdir(parents=True)
            (java/'service').mkdir()
            (java/'controller/Example.java').write_text(controller)
            (java/'service/Example.java').write_text(service)
            return violations(root)

    def test_rejects_entity_import(self):
        self.assertTrue(self.check('import com.reservedbytes.cinema_reservation.model.*;'))

    def test_rejects_database_call_through_service_getter(self):
        self.assertTrue(self.check('class Example { Object x() { return service.movies().findAll(); } }'))

    def test_ignores_comments_and_string_literals(self):
        self.assertEqual([],self.check('class Example { /* JpaRepository */ String s="java.sql.Connection"; }'))

    def test_rejects_repository_getter(self):
        self.assertTrue(self.check('class Example {}','class Service { public MovieRepository movies() { return movies; } }'))

if __name__ == '__main__':
    unittest.main()
